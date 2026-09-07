package chisa.zhida.controller;

import chisa.zhida.agent.SupportAgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@RestController
@Profile("ollama")
@RequestMapping("/api/agent/support-agent")
public class AgentController {
    private final SupportAgentService agent;

    public AgentController(SupportAgentService agent) { this.agent = agent; }

    @GetMapping("/run")
    public SupportAgentService.AgentRun run(
            @RequestParam(defaultValue = "Spring AI 如何接入 Ollama？") String question,
            @RequestParam(defaultValue = "harness") String strategy) {
        return agent.run(question, strategy);
    }

    @GetMapping("/compare")
    public SupportAgentService.AgentComparison compare(
            @RequestParam(defaultValue = "Spring AI 如何接入 Ollama？") String question) {
        return agent.compare(question);
    }
}
