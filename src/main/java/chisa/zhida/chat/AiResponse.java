package chisa.zhida.chat;

public final class AiResponse {
    private final String v; private final String reasoning; private final boolean done;
    public AiResponse(String v, String reasoning, boolean done) { this.v = v; this.reasoning = reasoning; this.done = done; }
    public String v() { return v; }
    public String reasoning() { return reasoning; }
    public boolean done() { return done; }
    public String getV() { return v; }
    public String getReasoning() { return reasoning; }
    public boolean isDone() { return done; }
    public static AiResponse text(String value) { return new AiResponse(value, null, false); }
    public static AiResponse reasoning(String value) { return new AiResponse(null, value, false); }
    public static AiResponse end() { return new AiResponse(null, null, true); }
}
