package chisa.zhida.controller;

import chisa.zhida.chat.NetworkSearchService;
import chisa.zhida.chat.SearchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Capability endpoint for concurrent SearXNG page fetching and Jsoup cleanup. */
@RestController
@RequestMapping("/api/network")
public class NetworkController {
    private final NetworkSearchService network;
    public NetworkController(NetworkSearchService network) { this.network = network; }

    @GetMapping("/test")
    public List<SearchResult> test(@RequestParam String message,
                                   @RequestParam(defaultValue = "7") long timeoutSeconds) {
        return network.fetchPages(message, timeoutSeconds, TimeUnit.SECONDS).join();
    }
}
