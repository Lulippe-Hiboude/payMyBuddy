package com.lulippe.paymybuddy.utils;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class LogUtil {
    private static final Logger log = LoggerFactory.getLogger(LogUtil.class);

    public static void logRequestFailed(final String message) {
        log.error("register request failed: {}", message);
    }
}
