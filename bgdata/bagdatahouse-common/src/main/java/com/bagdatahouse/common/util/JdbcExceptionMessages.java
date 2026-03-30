package com.bagdatahouse.common.util;

/**
 * 将 JDBC / 网络类异常转换为用户可读的中文提示（避免把长堆栈或英文驱动原文直接暴露给前端）。
 */
public final class JdbcExceptionMessages {

    private static final String CONNECTION_HINT =
            "无法连接数据库，请检查数据源地址、端口、网络连通性及数据库服务是否已启动";

    private JdbcExceptionMessages() {
    }

    /**
     * 若异常链中含有典型的「连不上库」错误，返回统一提示；否则返回 null。
     */
    public static String connectionIssueHint(Throwable e) {
        if (e == null) {
            return null;
        }
        Throwable t = e;
        while (t != null) {
            String name = t.getClass().getName();
            String msg = t.getMessage() != null ? t.getMessage() : "";
            if (name.contains("CommunicationsException")
                    || name.contains("ConnectException")
                    || name.contains("SocketTimeoutException")
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused")
                    || msg.contains("Connection timed out")
                    || msg.contains("No route to host")
                    || msg.contains("could not connect to server")) {
                return CONNECTION_HINT;
            }
            t = t.getCause();
        }
        return null;
    }
}
