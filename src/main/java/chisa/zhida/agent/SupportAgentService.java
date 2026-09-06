package chisa.zhida.agent;

import chisa.zhida.knowledge.KnowledgeService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/** Bounded agent loop for the real capability knowledge-base support scenario. */
@Service
@Profile("ollama")
public class SupportAgentService {
    private final ChatClient chatClient;
    private final KnowledgeService knowledgeService;

    public SupportAgentService(ChatClient chatClient, KnowledgeService knowledgeService) {
        this.chatClient = chatClient;
        this.knowledgeService = knowledgeService;
    }

    public AgentRun run(String question, String strategy) {
        long started = System.nanoTime();
        List<StepMetric> steps = new ArrayList<>();
        String mode = "baseline".equalsIgnoreCase(strategy) ? "baseline" : "harness";
        String context = "";
        String answer;
        int modelCalls = 0;

        if ("harness".equals(mode)) {
            long retrievalStarted = System.nanoTime();
            context = knowledgeService.contextFor(question);
            steps.add(metric("retrieve", retrievalStarted, context.isBlank() ? "miss" : "hit"));

            long generationStarted = System.nanoTime();
            answer = generate(question, context, false);
            modelCalls++;
            steps.add(metric("answer", generationStarted, answer.isBlank() ? "empty" : "ok"));

            if (answer.length() < 20) {
                long retryStarted = System.nanoTime();
                answer = generate(question, context, true);
                modelCalls++;
                steps.add(metric("retry", retryStarted, answer.isBlank() ? "empty" : "ok"));
            }
        } else {
            long generationStarted = System.nanoTime();
            answer = generate(question, "", false);
            modelCalls++;
            steps.add(metric("direct-answer", generationStarted, answer.isBlank() ? "empty" : "ok"));
        }

        long totalMs = elapsedMs(started);
        Metrics metrics = new Metrics(steps.size(), modelCalls, "harness".equals(mode) ? 1 : 0,
                context.length(), answer.length(), totalMs, !answer.isBlank());
        return new AgentRun("项目知识库客服", mode, question, answer, metrics, steps);
    }

    public AgentComparison compare(String question) {
        AgentRun baseline = run(question, "baseline");
        AgentRun harness = run(question, "harness");
        return new AgentComparison("项目知识库客服", question, baseline, harness,
                harness.metrics().totalMs() - baseline.metrics().totalMs(),
                harness.metrics().modelCalls() - baseline.metrics().modelCalls(),
                harness.metrics().contextChars(),
                harness.metrics().answerChars() - baseline.metrics().answerChars());
    }

    private String generate(String question, String context, boolean retry) {
        String prompt = context.isBlank()
                ? question
                : "请只根据下面的智答ai项目知识库回答。资料不足时明确说明，不要编造。\n\n知识库：\n"
                + context + "\n\n问题：\n" + question;
        if (retry) prompt += "\n\n上一次回答过短，请补充清晰的操作步骤。";
        String answer = chatClient.prompt().user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "agent-support-agent"))
                .call().content();
        return answer == null ? "" : answer.trim();
    }

    private StepMetric metric(String name, long started, String result) {
        return new StepMetric(name, elapsedMs(started), result);
    }

    private long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }

    public record AgentRun(String scene, String strategy, String question, String answer,
                           Metrics metrics, List<StepMetric> steps) {}
    public record AgentComparison(String scene, String question, AgentRun baseline, AgentRun harness,
                                  long latencyDeltaMs, int modelCallDelta, int groundedContextChars,
                                  int answerCharsDelta) {}
    public record Metrics(int rounds, int modelCalls, int retrievalCalls, int contextChars,
                          int answerChars, long totalMs, boolean success) {}
    public record StepMetric(String step, long elapsedMs, String result) {}
}
