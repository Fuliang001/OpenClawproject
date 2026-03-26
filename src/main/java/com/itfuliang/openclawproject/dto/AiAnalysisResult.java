package com.itfuliang.openclawproject.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 【关键】忽略 JSON 中多余未知的字段 (如 code, msg)
public class AiAnalysisResult {

    // 如果 AI 返回了 code 和 msg，你也可以把它们加上，或者直接用 ignoreUnknown 忽略
    private Integer code;
    private String msg;
    private Boolean success;

    private List<Item> data;

    public boolean isSuccess() {
        return code == 200;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String type;      // SCHEDULE 或 TODO
        private String title;     // 标题
        private String time;      // 时间
        private String location;  // 地点 (日程用)
        private String priority;  // 优先级 (待办用)
    }
}



//package com.itfuliang.openclawproject.dto;
//
//import lombok.Data;
//import java.util.List;
//
//@Data
//public class AiAnalysisResult {
//    private boolean success;
//    private List<Item> data;
//
//    @Data
//    public static class Item {
//        private String type; // SCHEDULE or TODO
//        private String title;
//        private String time;
//        private String location;
//        private String priority;
//    }
//}