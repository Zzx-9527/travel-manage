package com.zjx.travel.util;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 门票可售窗口规则（纯函数，便于单测，不依赖 Spring/数据库）。
 *
 * <p>以 Asia/Shanghai 自然日为准：可售范围是含当天的 30 个自然日，
 * 即 [today, today+29]；当天到达景区停售时间后不再接单，未来日期不受当天停售时间限制。
 */
public final class TicketSaleWindow {

    /** 可售天数（含当天） */
    public static final int SELLABLE_DAYS = 30;

    private TicketSaleWindow() {
    }

    /** 日期是否落在可售窗口内；过去日期与第 31 天及以后都返回 false */
    public static boolean isSellableDate(LocalDate date, LocalDate today) {
        if (date == null || today == null) {
            return false;
        }
        return !date.isBefore(today) && !date.isAfter(today.plusDays(SELLABLE_DAYS - 1));
    }

    /**
     * 当天票是否已过停售时间。
     *
     * @param date    游玩日期
     * @param today   服务端当前自然日
     * @param now     服务端当前时间
     * @param cutoff  景区停售时间（HH:mm 或 HH:mm:ss），为空或格式非法时不拦截
     * @return true 表示当天票已停止售卖
     */
    public static boolean isClosedForToday(LocalDate date, LocalDate today, LocalTime now, String cutoff) {
        if (date == null || today == null || now == null || !date.equals(today) || cutoff == null) {
            return false;
        }
        LocalTime close;
        try {
            close = LocalTime.parse(cutoff);
        } catch (RuntimeException e) {
            return false;
        }
        return !now.isBefore(close);
    }
}
