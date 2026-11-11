package com.example.main.service;

import com.example.main.entity.Cinema;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ChatbotLocationMatcher {

    public record LocationAreaRule(String canonicalLabel, List<String> aliases, List<String> cinemaKeywords) {
    }

    private static final List<LocationAreaRule> AREA_RULES = buildAreaRules();
    private static final Set<String> GENERIC_CINEMA_BRAND_TOKENS = Set.of(
            "cgv", "bhd", "star", "lotte", "galaxy", "cinestar", "beta", "vincom", "aeon", "mall", "plaza", "center", "centre", "go"
    );

    /**
     * Trích xuất tên khu vực chuẩn từ câu nhập của người dùng (ưu tiên cụm từ dài hơn trước).
     */
    public String extractLocationFromText(String rawText) {
        String normalized = ChatbotTextNormalizer.normalize(rawText);
        if (normalized.isBlank()) {
            return null;
        }

        // Chuẩn hóa một số dạng viết tắt quận như "q.1", "q 7", "q10" -> đã thành "q 1" hoặc "q1" sau normalize
        String bestLabel = null;
        int bestLength = -1;

        for (LocationAreaRule rule : AREA_RULES) {
            for (String alias : rule.aliases()) {
                if (ChatbotTextNormalizer.containsWholePhrase(normalized, alias)) {
                    if (alias.length() > bestLength) {
                        bestLength = alias.length();
                        bestLabel = rule.canonicalLabel();
                    }
                }
            }
        }
        return bestLabel;
    }

    /**
     * Trích xuất tên địa điểm ngay cả khi người dùng nhập tên rạp cụ thể có trong DB (VD: "Royal City", "Crescent Mall").
     */
    public String extractLocationOrCinemaFromText(String rawText, List<Cinema> allCinemas) {
        String areaLabel = extractLocationFromText(rawText);
        if (areaLabel != null) {
            return areaLabel;
        }

        String normalized = ChatbotTextNormalizer.normalize(rawText);
        if (normalized.isBlank() || allCinemas == null) {
            return null;
        }

        String bestCinemaMatch = null;
        int bestScore = 0;

        for (Cinema cinema : allCinemas) {
            if (cinema == null || cinema.getName() == null) {
                continue;
            }
            String normCinema = ChatbotTextNormalizer.normalize(cinema.getName());
            String strippedCinema = stripBrandPrefix(normCinema);
            if (strippedCinema.length() >= 4 && ChatbotTextNormalizer.containsWholePhrase(normalized, strippedCinema)) {
                if (strippedCinema.length() > bestScore) {
                    bestScore = strippedCinema.length();
                    bestCinemaMatch = cinema.getName();
                }
            } else if (normCinema.length() >= 6 && ChatbotTextNormalizer.containsWholePhrase(normalized, normCinema)) {
                if (normCinema.length() > bestScore) {
                    bestScore = normCinema.length();
                    bestCinemaMatch = cinema.getName();
                }
            }
        }

        return bestCinemaMatch;
    }

    /**
     * Tìm các rạp chiếu phim trong DB phù hợp với khu vực/địa điểm người dùng yêu cầu.
     */
    public List<Cinema> findMatchingCinemas(String location, List<Cinema> allCinemas) {
        if (location == null || location.isBlank() || allCinemas == null || allCinemas.isEmpty()) {
            return List.of();
        }

        String normLocation = ChatbotTextNormalizer.normalize(location);
        List<String> keywords = new ArrayList<>();

        // 1. Khớp chính xác theo canonicalLabel của 1 rule duy nhất (tránh "Cầu Giấy (Hà Nội)" khớp lan sang "Hà Nội")
        LocationAreaRule exactRule = null;
        for (LocationAreaRule rule : AREA_RULES) {
            if (ChatbotTextNormalizer.normalize(rule.canonicalLabel()).equals(normLocation)) {
                exactRule = rule;
                break;
            }
        }

        // 2. Nếu không truyền canonicalLabel mà truyền tên thô (VD "Quận 7"), chọn 1 rule có alias khớp dài nhất
        if (exactRule == null) {
            int bestAliasLen = -1;
            for (LocationAreaRule rule : AREA_RULES) {
                for (String alias : rule.aliases()) {
                    if (ChatbotTextNormalizer.containsWholePhrase(normLocation, alias) && alias.length() > bestAliasLen) {
                        bestAliasLen = alias.length();
                        exactRule = rule;
                    }
                }
            }
        }

        if (exactRule != null) {
            keywords.addAll(exactRule.cinemaKeywords());
        }

        List<Cinema> matched = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (Cinema cinema : allCinemas) {
            if (cinema == null || cinema.getName() == null) {
                continue;
            }
            String normCinema = ChatbotTextNormalizer.normalize(cinema.getName());

            boolean isMatch = false;
            if (!keywords.isEmpty()) {
                for (String kw : keywords) {
                    if (matchesCinemaKeyword(normCinema, kw)) {
                        isMatch = true;
                        break;
                    }
                }
            }

            // Hỗ trợ khớp trực tiếp nếu user nhập tên địa danh / trung tâm thương mại trùng với tên rạp
            if (!isMatch && normLocation.length() >= 3 && !GENERIC_CINEMA_BRAND_TOKENS.contains(normLocation)) {
                if (ChatbotTextNormalizer.containsWholePhrase(normCinema, normLocation)) {
                    isMatch = true;
                }
            }

            if (isMatch && seenIds.add(cinema.getId())) {
                matched.add(cinema);
            }
        }

        return matched;
    }

    private boolean matchesCinemaKeyword(String normCinema, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        if (ChatbotTextNormalizer.containsWholePhrase(normCinema, keyword)) {
            return true;
        }
        // Riêng các cụm nhiều từ như "ha noi", "ho chi minh" cho phép chứa trực tiếp
        return keyword.contains(" ") && normCinema.contains(keyword);
    }

    private String stripBrandPrefix(String normCinema) {
        String[] tokens = normCinema.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!GENERIC_CINEMA_BRAND_TOKENS.contains(token)) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(token);
            }
        }
        return sb.toString().trim();
    }

    private static List<LocationAreaRule> buildAreaRules() {
        List<LocationAreaRule> rules = new ArrayList<>();

        // --- HÀ NỘI & CÁC QUẬN ---
        rules.add(new LocationAreaRule(
                "Cầu Giấy (Hà Nội)",
                List.of("cau giay", "q cau giay", "quan cau giay", "iph", "tran duy hung", "dich vong"),
                List.of("discovery", "indochina", "tran duy hung", "sky lake", "the garden", "centerpoint")
        ));
        rules.add(new LocationAreaRule(
                "Đống Đa (Hà Nội)",
                List.of("dong da", "q dong da", "quan dong da", "pham ngoc thach", "nguyen chi thanh", "lang ha"),
                List.of("pham ngoc thach", "nguyen chi thanh", "quoc gia", "hanoi centre")
        ));
        rules.add(new LocationAreaRule(
                "Thanh Xuân (Hà Nội)",
                List.of("thanh xuan", "q thanh xuan", "quan thanh xuan", "royal city", "nga tu so"),
                List.of("royal city", "centerpoint")
        ));
        rules.add(new LocationAreaRule(
                "Hai Bà Trưng (Hà Nội)",
                List.of("hai ba trung", "q hai ba trung", "quan hai ba trung", "ba trieu", "times city", "minh khai", "bach mai"),
                List.of("ba trieu", "times city", "luong yen", "truong dinh")
        ));
        rules.add(new LocationAreaRule(
                "Hoàn Kiếm (Hà Nội)",
                List.of("hoan kiem", "q hoan kiem", "quan hoan kiem", "bo ho", "ho guom", "trang tien", "pho co"),
                List.of("trang tien", "ba trieu")
        ));
        rules.add(new LocationAreaRule(
                "Ba Đình (Hà Nội)",
                List.of("ba dinh", "q ba dinh", "quan ba dinh", "lieu giai", "kim ma"),
                List.of("lieu giai", "metropolis", "quoc gia", "hanoi centre")
        ));
        rules.add(new LocationAreaRule(
                "Tây Hồ (Hà Nội)",
                List.of("tay ho", "q tay ho", "quan tay ho", "ho tay", "thuy khue", "xuan dieu", "lac long quan"),
                List.of("west lake", "thuy khue", "xuan dieu", "kosmo")
        ));
        rules.add(new LocationAreaRule(
                "Long Biên (Hà Nội)",
                List.of("long bien", "q long bien", "quan long bien", "gia lam"),
                List.of("long bien", "ocean park", "ocean city")
        ));
        rules.add(new LocationAreaRule(
                "Hà Đông (Hà Nội)",
                List.of("ha dong", "q ha dong", "quan ha dong", "van quan", "mo lao"),
                List.of("ha dong", "ho guom plaza", "mac plaza", "machinco", "parkcity")
        ));
        rules.add(new LocationAreaRule(
                "Hoàng Mai (Hà Nội)",
                List.of("hoang mai", "q hoang mai", "quan hoang mai", "linh dam", "truong dinh", "giap bat"),
                List.of("rice city", "truong dinh", "times city")
        ));
        rules.add(new LocationAreaRule(
                "Nam Từ Liêm / Mỹ Đình (Hà Nội)",
                List.of("nam tu liem", "my dinh", "me tri", "keangnam"),
                List.of("the garden", "sky lake")
        ));
        rules.add(new LocationAreaRule(
                "Bắc Từ Liêm (Hà Nội)",
                List.of("bac tu liem", "co nhue", "xuan dinh"),
                List.of("bac tu liem", "kosmo")
        ));
        rules.add(new LocationAreaRule(
                "Đan Phượng (Hà Nội)",
                List.of("dan phuong"),
                List.of("dan phuong")
        ));
        rules.add(new LocationAreaRule(
                "Hà Nội",
                List.of("ha noi", "hanoi", "hn", "thu do"),
                List.of(
                        "ha noi", "hanoi", "royal city", "times city", "ba trieu", "nguyen chi thanh",
                        "trang tien", "lieu giai", "metropolis", "tran duy hung", "sky lake",
                        "bac tu liem", "ocean park", "ocean city", "long bien", "ha dong",
                        "ho guom plaza", "mac plaza", "machinco", "rice city", "luong yen",
                        "thuy khue", "truong dinh", "xuan dieu", "west lake", "kosmo",
                        "discovery", "the garden", "pham ngoc thach", "quoc gia", "centerpoint",
                        "parkcity", "dan phuong", "ecopark", "mipec"
                )
        ));

        // --- TP. HỒ CHÍ MINH & CÁC QUẬN ---
        rules.add(new LocationAreaRule(
                "Quận 1 (TP.HCM)",
                List.of("quan 1", "q1", "q 1", "ben thanh", "nguyen hue", "dong khoi", "bui vien"),
                List.of("dong khoi", "liberty", "quoc thanh", "nguyen du")
        ));
        rules.add(new LocationAreaRule(
                "Quận 3 (TP.HCM)",
                List.of("quan 3", "q3", "q 3", "ly chinh thang"),
                List.of("ly chinh thang", "quoc thanh", "nguyen du")
        ));
        rules.add(new LocationAreaRule(
                "Quận 4 (TP.HCM)",
                List.of("quan 4", "q4", "q 4", "ben van don"),
                List.of("gold view")
        ));
        rules.add(new LocationAreaRule(
                "Quận 5 (TP.HCM)",
                List.of("quan 5", "q5", "q 5", "cho lon"),
                List.of("hung vuong plaza")
        ));
        rules.add(new LocationAreaRule(
                "Quận 6 (TP.HCM)",
                List.of("quan 6", "q6", "q 6"),
                List.of("satra quan 6", "kinh duong vuong")
        ));
        rules.add(new LocationAreaRule(
                "Quận 7 (TP.HCM)",
                List.of("quan 7", "q7", "q 7", "phu my hung", "nguyen van linh"),
                List.of("crescent mall", "vivo city", "nam sai gon", "huynh tan phat")
        ));
        rules.add(new LocationAreaRule(
                "Quận 8 / Bình Chánh (TP.HCM)",
                List.of("quan 8", "q8", "q 8", "binh chanh", "ta quang buu"),
                List.of("parc mall", "bhd star pham hung")
        ));
        rules.add(new LocationAreaRule(
                "Quận 10 (TP.HCM)",
                List.of("quan 10", "q10", "q 10", "su van hanh", "ba thang hai"),
                List.of("su van hanh", "3 2")
        ));
        rules.add(new LocationAreaRule(
                "Quận 11 (TP.HCM)",
                List.of("quan 11", "q11", "q 11", "dam sen"),
                List.of("hung vuong plaza", "su van hanh", "3 2")
        ));
        rules.add(new LocationAreaRule(
                "Quận 12 / Hóc Môn / Củ Chi (TP.HCM)",
                List.of("quan 12", "q12", "q 12", "hoc mon", "cu chi"),
                List.of("nguyen van qua", "trung chanh", "cu chi")
        ));
        rules.add(new LocationAreaRule(
                "Bình Thạnh (TP.HCM)",
                List.of("binh thanh", "q binh thanh", "quan binh thanh", "landmark 81", "hang xanh", "nguyen xi"),
                List.of("landmark 81", "pearl plaza", "saigonres", "nguyen xi")
        ));
        rules.add(new LocationAreaRule(
                "Phú Nhuận (TP.HCM)",
                List.of("phu nhuan", "q phu nhuan", "quan phu nhuan", "phan xich long"),
                List.of("hoang van thu", "pearl plaza", "menas", "nguyen du")
        ));
        rules.add(new LocationAreaRule(
                "Tân Bình (TP.HCM)",
                List.of("tan binh", "q tan binh", "quan tan binh", "san bay", "tan son nhat", "cong hoa"),
                List.of("menas", "ct plaza", "hoang van thu", "tan binh", "cong hoa", "truong chinh")
        ));
        rules.add(new LocationAreaRule(
                "Tân Phú (TP.HCM)",
                List.of("tan phu", "q tan phu", "quan tan phu", "celadon"),
                List.of("aeon tan phu", "pandora", "truong chinh")
        ));
        rules.add(new LocationAreaRule(
                "Gò Vấp (TP.HCM)",
                List.of("go vap", "q go vap", "quan go vap", "phan huy ich", "quang trung"),
                List.of("go vap", "quang trung", "phan huy ich")
        ));
        rules.add(new LocationAreaRule(
                "Thủ Đức (TP.HCM)",
                List.of("thu duc", "tp thu duc", "quan 2", "q2", "q 2", "quan 9", "q9", "q 9", "thao dien", "linh trung", "lang dai hoc"),
                List.of("thu duc", "gigamall", "thao dien", "le van viet", "grand park", "hiep phu", "sinh vien", "linh trung", "sala", "cantavil", "moonlight")
        ));
        rules.add(new LocationAreaRule(
                "Bình Tân (TP.HCM)",
                List.of("binh tan", "q binh tan", "quan binh tan", "an lac", "ten lua"),
                List.of("binh tan", "an lac")
        ));
        rules.add(new LocationAreaRule(
                "TP. Hồ Chí Minh",
                List.of("tp hcm", "tphcm", "ho chi minh", "tp ho chi minh", "sai gon", "saigon", "hcm", "sg"),
                List.of(
                        "dong khoi", "liberty", "quoc thanh", "nguyen du", "ly chinh thang", "gold view",
                        "hung vuong plaza", "satra quan 6", "kinh duong vuong", "crescent mall", "vivo city",
                        "nam sai gon", "huynh tan phat", "bhd star pham hung", "parc mall", "su van hanh",
                        "3 2", "menas", "ct plaza", "hoang van thu", "tan binh", "cong hoa", "truong chinh",
                        "aeon tan phu", "pandora", "landmark 81", "pearl plaza", "saigonres", "nguyen xi",
                        "quang trung", "go vap", "phan huy ich", "thao dien", "le van viet", "gigamall",
                        "thu duc", "grand park", "hiep phu", "sinh vien", "linh trung", "sala", "cantavil",
                        "moonlight", "binh tan", "an lac", "nguyen van qua", "trung chanh", "cu chi"
                )
        ));

        // --- CÁC TỈNH / THÀNH PHỐ KHÁC ---
        rules.add(new LocationAreaRule("Đà Nẵng", List.of("da nang", "danang", "dn", "thanh khe", "hai chau", "son tra"), List.of("da nang", "thanh khe", "vinh trung plaza")));
        rules.add(new LocationAreaRule("Hải Phòng", List.of("hai phong", "haiphong", "hp"), List.of("hai phong")));
        rules.add(new LocationAreaRule("Huế", List.of("hue", "tp hue", "thua thien hue"), List.of("bhd star hue", "cinestar hue", "lotte hue")));
        rules.add(new LocationAreaRule("Cần Thơ", List.of("can tho", "ninh kieu", "cai rang"), List.of("can tho", "sense city", "xuan khanh", "vincom hung vuong")));
        rules.add(new LocationAreaRule("Bình Dương", List.of("binh duong", "di an", "thuan an", "thu dau mot"), List.of("binh duong", "canary", "di an", "sinh vien")));
        rules.add(new LocationAreaRule("Đồng Nai / Biên Hòa", List.of("dong nai", "bien hoa", "long khanh"), List.of("dong nai", "bien hoa", "long khanh")));
        rules.add(new LocationAreaRule("Vũng Tàu", List.of("vung tau", "ba ria", "ba ria vung tau"), List.of("vung tau", "lam son square", "phu my")));
        rules.add(new LocationAreaRule("Nha Trang (Khánh Hòa)", List.of("nha trang", "khanh hoa"), List.of("nha trang", "gold coast")));
        rules.add(new LocationAreaRule("Đà Lạt (Lâm Đồng)", List.of("da lat", "lam dong", "bao loc"), List.of("da lat", "lam dong", "bao loc")));
        rules.add(new LocationAreaRule("Hạ Long (Quảng Ninh)", List.of("quang ninh", "ha long", "cam pha", "mong cai"), List.of("ha long", "cam pha", "mong cai")));
        rules.add(new LocationAreaRule("Thanh Hóa", List.of("thanh hoa", "sam son"), List.of("thanh hoa", "sam son")));
        rules.add(new LocationAreaRule("Vinh (Nghệ An)", List.of("nghe an", "tp vinh", "vinh"), List.of("vinh centre", "lotte vinh")));
        rules.add(new LocationAreaRule("Việt Trì (Phú Thọ)", List.of("viet tri", "phu tho"), List.of("viet tri", "phu tho")));
        rules.add(new LocationAreaRule("Hưng Yên", List.of("hung yen", "ecopark"), List.of("ecopark", "ocean city")));
        rules.add(new LocationAreaRule("Bắc Giang", List.of("bac giang"), List.of("bac giang")));
        rules.add(new LocationAreaRule("Bắc Ninh", List.of("bac ninh"), List.of("bac ninh")));
        rules.add(new LocationAreaRule("Buôn Ma Thuột", List.of("buon ma thuot", "buon me thuot", "dak lak"), List.of("buon me thuot")));
        rules.add(new LocationAreaRule("Quy Nhơn (Bình Định)", List.of("quy nhon", "binh dinh"), List.of("kim cuc plaza")));
        rules.add(new LocationAreaRule("Mỹ Tho (Tiền Giang)", List.of("my tho", "tien giang"), List.of("my tho")));
        rules.add(new LocationAreaRule("Thái Nguyên", List.of("thai nguyen"), List.of("vincom thai nguyen")));
        rules.add(new LocationAreaRule("Nam Định", List.of("nam dinh"), List.of("nam dinh")));
        rules.add(new LocationAreaRule("Ninh Bình", List.of("ninh binh"), List.of("ninh binh")));
        rules.add(new LocationAreaRule("Hải Dương", List.of("hai duong"), List.of("hai duong")));
        rules.add(new LocationAreaRule("Hội An", List.of("hoi an", "quang nam"), List.of("hoi an")));
        rules.add(new LocationAreaRule("Phan Thiết", List.of("phan thiet", "binh thuan"), List.of("phan thiet")));
        rules.add(new LocationAreaRule("Phan Rang", List.of("phan rang", "ninh thuan"), List.of("phan rang")));
        rules.add(new LocationAreaRule("Cà Mau", List.of("ca mau"), List.of("ca mau")));
        rules.add(new LocationAreaRule("Long Xuyên", List.of("long xuyen", "an giang"), List.of("long xuyen")));
        rules.add(new LocationAreaRule("Đồng Hới", List.of("dong hoi", "quang binh"), List.of("dong hoi")));
        rules.add(new LocationAreaRule("Phủ Lý (Hà Nam)", List.of("phu ly", "ha nam"), List.of("phu ly")));
        rules.add(new LocationAreaRule("Thái Bình", List.of("thai binh"), List.of("thai binh")));
        rules.add(new LocationAreaRule("Tuyên Quang", List.of("tuyen quang"), List.of("tuyen quang")));
        rules.add(new LocationAreaRule("Hà Tĩnh", List.of("ha tinh"), List.of("ha tinh")));
        rules.add(new LocationAreaRule("Kon Tum", List.of("kon tum"), List.of("kon tum")));
        rules.add(new LocationAreaRule("Lạng Sơn", List.of("lang son"), List.of("lang son")));
        rules.add(new LocationAreaRule("Phú Yên", List.of("phu yen", "tuy hoa"), List.of("phu yen")));
        rules.add(new LocationAreaRule("Quảng Ngãi", List.of("quang ngai"), List.of("quang ngai")));
        rules.add(new LocationAreaRule("Rạch Giá (Kiên Giang)", List.of("rach gia", "kien giang"), List.of("rach gia", "kien giang")));
        rules.add(new LocationAreaRule("Sóc Trăng", List.of("soc trang"), List.of("soc trang")));
        rules.add(new LocationAreaRule("Sơn La", List.of("son la"), List.of("son la")));
        rules.add(new LocationAreaRule("Tây Ninh", List.of("tay ninh"), List.of("tay ninh")));
        rules.add(new LocationAreaRule("Trà Vinh", List.of("tra vinh"), List.of("tra vinh")));
        rules.add(new LocationAreaRule("Vị Thanh (Hậu Giang)", List.of("vi thanh", "hau giang"), List.of("vi thanh")));
        rules.add(new LocationAreaRule("Vĩnh Long", List.of("vinh long"), List.of("vinh long")));
        rules.add(new LocationAreaRule("Yên Bái", List.of("yen bai"), List.of("yen bai")));
        rules.add(new LocationAreaRule("Bạc Liêu", List.of("bac lieu"), List.of("bac lieu")));
        rules.add(new LocationAreaRule("Cao Lãnh (Đồng Tháp)", List.of("cao lanh", "dong thap"), List.of("cao lanh")));

        return List.copyOf(rules);
    }
}
