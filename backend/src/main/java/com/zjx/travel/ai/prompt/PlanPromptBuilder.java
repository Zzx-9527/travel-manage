package com.zjx.travel.ai.prompt;

import com.zjx.travel.ai.dto.AiPlanRequest;
import com.zjx.travel.domain.ScenicEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 行程规划的 Prompt 构建。
 *
 * <p>两个刻意的设计：
 * <ol>
 *   <li><b>把候选景点当成"封闭世界"塞进 Prompt</b>：模型只能从清单里选，从源头降低幻觉；
 *       服务端还会再做一次 id 白名单校验，形成两道闸。</li>
 *   <li><b>静态规则写 system，动态数据写 user</b>：DeepSeek 有自动上下文缓存，
 *       system 部分固定不变才能命中前缀缓存，长期调用能省一笔钱。</li>
 * </ol>
 */
@Component
public class PlanPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            你是一名资深旅游行程规划师。请根据用户需求，从给定的候选景点中挑选并安排多天行程。

            硬性规则：
            1. 只能使用候选清单中出现的景点，禁止编造清单之外的景点、id 或名称。
            2. 每天安排 1-4 个景点，返回的天数必须等于用户要求的天数。
            3. 同一个景点在整个行程中只能出现一次。候选景点不足以填满某一天时，
               宁可当天少排（甚至 spots 返回空数组表示自由活动），也绝对不允许重复使用景点。
            4. 每天的景点按 上午 → 下午 → 晚上 排列，timeSlot 只能取这三个值之一。
            5. 同一天的景点要考虑地理上的顺路，不要来回折返。
            6. traffic 写"从上一站到本站"的交通方式与大致耗时；当天第一站写从住处出发。
               能确定地铁/公交线路就写具体线路，不确定就写"打车约 20 分钟"这类描述，
               不要编造不存在的线路。
            7. intro 写 60-100 字的景点介绍（看点、背景、怎么逛）。不要写票价和开放时间，
               这些由系统提供。
            8. notice 写 1-2 条实用提醒（预约、闭馆日、排队高峰、穿着）。不确定的具体信息不要写，
               用通用提醒代替。
            9. 只输出 JSON，不要 markdown 代码块，不要任何解释文字。

            输出 JSON 结构：
            {
              "title": "行程标题，20 字以内",
              "intro": "整体思路，80 字以内，说明为什么这样安排",
              "days": [
                {
                  "dayNo": 1,
                  "theme": "当天主题，10 字以内",
                  "spots": [
                    {
                      "scenicId": 3,
                      "timeSlot": "上午",
                      "stayTime": "3小时",
                      "traffic": "地铁1号线天安门东站 B 口，步行约 8 分钟",
                      "intro": "明清两代的皇家宫殿，现存规模最大、保存最完整的木质结构古建筑群。建议沿中轴线从午门进、神武门出，重点看太和殿与珍宝馆。",
                      "notice": "周一闭馆；需提前在官网实名预约，旺季建议提前 7 天。",
                      "reason": "第一次来北京的核心地标"
                    }
                  ]
                }
              ],
              "tips": ["整体提示 1", "整体提示 2"]
            }
            """;

    /** 系统提示词：固定不变，便于命中模型的上下文缓存 */
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    /** 用户提示词：需求字段 + 候选景点清单 */
    public String userPrompt(AiPlanRequest request, List<ScenicEntity> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("目的地：").append(request.getDestination()).append('\n');
        sb.append("天数：").append(request.getDays()).append('\n');
        if (request.getBudget() != null) {
            sb.append("预算：").append(request.getBudget().intValue()).append(" 元\n");
        }
        if (hasText(request.getPreference())) {
            sb.append("偏好：").append(request.getPreference().trim()).append('\n');
        }
        if (hasText(request.getExtra())) {
            sb.append("补充说明：").append(request.getExtra().trim()).append('\n');
        }

        sb.append("\n候选景点（格式：id | 名称 | 地址 | 票价 | 评分）：\n");
        for (ScenicEntity scenic : candidates) {
            sb.append(scenic.getId()).append(" | ")
                    .append(scenic.getName()).append(" | ")
                    .append(scenic.getAddress() == null ? "-" : scenic.getAddress()).append(" | ")
                    .append(scenic.getPrice() == null ? "-" : scenic.getPrice().intValue()).append(" | ")
                    .append(scenic.getScore() == null ? "-" : scenic.getScore())
                    .append('\n');
        }
        sb.append("\n再次强调：spots 里的 scenicId 必须来自上面的候选清单，"
                + "清单之外的一律不要出现。");
        return sb.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
