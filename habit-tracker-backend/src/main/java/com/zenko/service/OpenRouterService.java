package com.zenko.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    // Read from environment variable OPENROUTER_API_KEY or application.properties
    @Value("${openrouter.api.key:${OPENROUTER_API_KEY:}}")
    private String apiKey;

    // OpenAI GPT-3.5 Turbo - reliable, very cheap (~$0.002/1K tokens)
    @Value("${openrouter.model:openai/gpt-3.5-turbo}")
    private String model;

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("=== OpenRouterService Configuration ===");
        log.info("OPENROUTER_API_KEY env var present: {}", System.getenv("OPENROUTER_API_KEY") != null);
        log.info("apiKey property length: {}", apiKey != null ? apiKey.length() : "null");
        log.info("model: {}", model);
        log.info("isConfigured(): {}", isConfigured());
        log.info("========================================");
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("YOUR_OPENROUTER_API_KEY_HERE");
    }

    public String generate(String prompt, String systemPrompt) {
        if (!isConfigured()) return null;

        try {
            // Build request body for OpenRouter chat completions
            ChatRequest chatReq = new ChatRequest(
                    model,
                    List.of(
                            new Message("system", systemPrompt != null ? systemPrompt : "You are a helpful AI assistant."),
                            new Message("user", prompt)
                    ),
                    0.7
            );
            String body = mapper.writeValueAsString(chatReq);

            // Debug log
            log.info("OpenRouter request: model={}, promptPreview={}", model, prompt.substring(0, Math.min(50, prompt.length())));

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(OPENROUTER_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30));

            // Optional headers for OpenRouter (helps with routing/analytics)
            // You can set these in properties if needed
            // reqBuilder.header("HTTP-Referer", "https://yourapp.com");
            // reqBuilder.header("X-Title", "Zenko Habit Tracker");

            HttpResponse<String> resp = http.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.error("OpenRouter API error ({}): {}", resp.statusCode(), resp.body());
                return null;
            }

            JsonNode root = mapper.readTree(resp.body());
            log.debug("OpenRouter response body: {}", resp.body());

            // Check for error in response body (even with 200, OpenRouter might return error)
            if (root.has("error")) {
                log.error("OpenRouter API error in response: {}", root.get("error").asText());
                return null;
            }

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                log.error("OpenRouter response missing choices array: {}", resp.body());
                return null;
            }

            JsonNode message = choices.get(0).path("message");
            if (message.isMissingNode() || message.isNull()) {
                log.error("OpenRouter response missing message object: {}", resp.body());
                return null;
            }

            JsonNode content = message.path("content");
            if (content.isMissingNode() || content.isNull()) {
                log.error("OpenRouter response missing content: {}", resp.body());
                return null;
            }

            return content.asText();

        } catch (Exception e) {
            log.error("Error calling OpenRouter", e);
            return null;
        }
    }

    // ── AI methods ──
    // If API key configured, call OpenRouter; otherwise fall back to demo

    public String habitAutopsy(String habitName, int brokenStreak, String reason) {
        if (!isConfigured()) return demoAutopsy(habitName, brokenStreak);
        String prompt = String.format(
                "You are a habit coach AI. A user's habit \"%s\" just broke after a %d-day streak. " +
                "Reason given: \"%s\". Write a short, warm, personal 'Habit Autopsy' report in 3 sections: " +
                "1) What likely went wrong (2 sentences, specific not generic), " +
                "2) The root cause (1 sentence), " +
                "3) Three concrete actionable fixes for next week. " +
                "Use markdown. Be empathetic but honest. No fluff.",
                habitName, brokenStreak, reason == null ? "unknown" : reason);
        String result = generate(prompt, null);
        return result != null ? result : demoAutopsy(habitName, brokenStreak);
    }

    public String habitDNA(String completionSummary, String userName) {
        if (!isConfigured()) return demoDNA(userName);
        String prompt = String.format(
                "You are a habit analytics AI. Analyze this user's habit completion data and generate their personal 'Habit DNA Profile'. " +
                "Data: %s. User: %s. " +
                "Write: 1) Their habit personality type with a creative name (e.g. 'Morning Sprinter', 'Night Owl Achiever'). " +
                "2) Their 3 biggest strengths. 3) Their 3 growth areas. 4) Top 3 custom strategies just for them. " +
                "5) A motivational tagline. Use markdown with emojis. Be specific, not generic.",
                completionSummary, userName);
        String result = generate(prompt, null);
        return result != null ? result : demoDNA(userName);
    }

    public String predictiveAlert(String habitName, int recentMissRate, String timePattern) {
        if (!isConfigured()) return demoAlert(habitName);
        String prompt = String.format(
                "Habit coach AI. The user's habit '%s' has a %d%% miss rate over last 7 days. " +
                "Their usual completion pattern: %s. " +
                "Write a short, friendly nudge (3-4 sentences) to send 2 hours before their usual window closes. " +
                "Include: why now matters, one specific tip, an encouraging line. No generic advice.",
                habitName, recentMissRate, timePattern);
        String result = generate(prompt, null);
        return result != null ? result : demoAlert(habitName);
    }

    public String generateInsights(String habitSummary, String userName) {
        if (!isConfigured()) return demoInsightsJson();
        String prompt = String.format(
                "You are an AI habit coach. Analyze this habit data for user %s: %s. " +
                "Generate 4 short, personalized insights as JSON array with fields: " +
                "type (tip/warning/success/insight), title (short), body (2 sentences, specific). " +
                "Return ONLY valid JSON array, no markdown wrapper.",
                userName, habitSummary);
        String result = generate(prompt, null);
        return result != null ? result : demoInsightsJson();
    }

    public String generateQuests(String habitsSummary) {
        if (!isConfigured()) return demoQuestsJson();
        String prompt = String.format(
                "You are a gamification AI. Based on these user habits: %s. " +
                "Generate 3 weekly habit quests as JSON array with fields: " +
                "id (number), title (quest name ≤6 words), description (1 sentence challenge), " +
                "xpReward (number 50-200), difficulty (Easy/Medium/Hard), emoji (1 emoji). " +
                "Make them specific to the user's actual habits. Return ONLY valid JSON array.",
                habitsSummary);
        String result = generate(prompt, null);
        return result != null ? result : demoQuestsJson();
    }

    public String chatReply(String message, String habitContext, String userName) {
        if (!isConfigured()) return demoChatReply(message, habitContext);
        String prompt = String.format(
                "You are an expert AI habit coach named HabitFlow Coach. You are warm, concise, and science-backed.\n" +
                "User's name: %s\n" +
                "Their current habits & stats: %s\n\n" +
                "User message: \"%s\"\n\n" +
                "Reply in 2-4 short sentences. Be specific to their habits if relevant. " +
                "Use 1-2 relevant emojis. No markdown headers. Be conversational and encouraging.",
                userName,
                habitContext.isEmpty() ? "No habits tracked yet" : habitContext,
                message);
        String result = generate(prompt, null);
        return result != null ? result : demoChatReply(message, habitContext);
    }

    // ── Demo implementations ── (unchanged from GeminiService)

    // ── Demo implementations (same as before) ──

    private String demoAutopsy(String habit, int streak) {
        return "## 🔬 Habit Autopsy: " + habit + "\n\n" +
                "**What Went Wrong**\n\nAfter " + streak + " days, your momentum likely hit a friction point — " +
                "either the habit time conflicted with a new obligation, or the initial excitement faded without a deeper 'why'. " +
                "When habits break around the 2-3 week mark, it's usually an identity mismatch, not a willpower problem.\n\n" +
                "**Root Cause**\n\nThe habit wasn't yet anchored to a fixed trigger in your daily routine.\n\n" +
                "**3 Fixes for Next Week**\n\n" +
                "1. **Habit stack it** — attach it immediately after an existing habit you never miss\n" +
                "2. **Shrink it** — do 20% of the original habit. Consistency beats perfection\n" +
                "3. **Set a specific time** — 'I will do this at 7:30am' outperforms 'I will do this in the morning' by 3x";
    }

    private String demoDNA(String user) {
        return "## 🧬 Habit DNA Profile: " + user + "\n\n" +
                "### 🎯 Personality Type: **The Momentum Builder**\n\n" +
                "You thrive with visible progress and short-term wins. Once you get going, you're nearly unstoppable — " +
                "but cold starts are your kryptonite.\n\n" +
                "### 💪 Strengths\n- Excellent at maintaining mid-week consistency\n" +
                "- Strong at habit chaining once a routine is established\n- Great at bouncing back after short breaks\n\n" +
                "### 📈 Growth Areas\n- Weekend consistency drops significantly\n" +
                "- Monday re-starts take 2-3 days to rebuild\n- Need external accountability for new habits\n\n" +
                "### 🗺️ Your Custom Strategy\n1. Pre-commit Sunday evening for the week ahead\n" +
                "2. Use lighter 'maintenance mode' versions on weekends\n" +
                "3. Track streaks visually — your brain loves the chain\n\n" +
                "**Your tagline:** *\"I don't skip twice.\"*";
    }

    private String demoAlert(String habit) {
        return "⚡ Hey! Your '" + habit + "' window closes in 2 hours and you haven't checked in today. " +
                "Your discipline score drops 8% for every missed day this week. " +
                "Even 5 minutes counts — tiny action protects the streak. You've built something real. Don't let today be the break.";
    }

    private String demoInsights(String user) {
        return "[{\"type\":\"tip\",\"title\":\"Stack Your Morning\",\"body\":\"Pair your morning habit with coffee. " +
                "Habit stacking cuts friction by 60% — no willpower required.\"}," +
                "{\"type\":\"success\",\"title\":\"Consistency Champion\",\"body\":\"You've maintained above 70% completion this week. " +
                "That puts you in the top 15% of HabitFlow users.\"}," +
                "{\"type\":\"warning\",\"title\":\"Weekend Dip Detected\",\"body\":\"Your completion rate drops 40% on weekends. " +
                "Try a lighter version on Sat/Sun — half the habit beats zero.\"}," +
                "{\"type\":\"insight\",\"title\":\"Your Power Hour\",\"body\":\"Based on your check-in times, 7-8am is your peak performance window. " +
                "Front-load critical habits there.\"}]";
    }

    private String demoInsightsJson() { return demoInsights("User"); }

    private String demoQuestsJson() {
        return "[{\"id\":1,\"title\":\"Dawn Warrior\",\"description\":\"Complete all morning habits before 8am for 3 consecutive days\"," +
                "\"xpReward\":150,\"difficulty\":\"Medium\",\"emoji\":\"🌅\"}," +
                "{\"id\":2,\"title\":\"Perfect Week\",\"description\":\"Hit 100% habit completion for 5 out of 7 days this week\"," +
                "\"xpReward\":200,\"difficulty\":\"Hard\",\"emoji\":\"⚡\"}," +
                "{\"id\":3,\"title\":\"Streak Starter\",\"description\":\"Build a new 3-day streak on any habit you missed last week\"," +
                "\"xpReward\":75,\"difficulty\":\"Easy\",\"emoji\":\"🔥\"}]";
    }

    private String demoChatReply(String message, String ctx) {
        String q = message.toLowerCase();
        if (q.contains("streak"))   return "Streaks are the backbone of habit formation! 🔥 Missing once is human — the golden rule is never miss twice. Your longest streak shows what you're truly capable of!";
        if (q.contains("motivat")) return "Motivation follows action, not the other way around. 💪 Start the habit for just 2 minutes — momentum builds from there. Even tiny wins release dopamine!";
        if (q.contains("sleep"))    return "Sleep is the #1 habit that amplifies all others. 😴 Aim for 7-9 hours consistently. Even 30 min earlier to bed compoundss into massive energy gains over weeks.";
        if (q.contains("miss") || q.contains("fail") || q.contains("skip")) return "One missed day is a lapse, not a failure! ✨ Research shows a single miss has almost no impact on long-term success. What matters is what you do tomorrow — so make it count.";
        if (q.contains("tips") || q.contains("advice") || q.contains("help")) return "Top habit science: 1️⃣ Habit stack — pair new habits with existing ones. 2️⃣ Make it tiny — 2-minute versions build the chain. 3️⃣ Celebrate immediately — the feeling after is your reward!";
        if (q.contains("add") || q.contains("new habit")) return "To add a new habit, tap '+ Add Habit' at the top! 🎯 Start with just 1-2 habits max. Research shows willpower is limited — focus beats quantity every time.";
        if (q.contains("best") || q.contains("recommend")) return "Based on habit science: morning habits have the highest completion rates (33% higher). 🌅 Stack them right after your coffee or shower for automatic execution!";
        if (q.contains("progress") || q.contains("how am i")) return "Progress is made one day at a time! 📈 Focus on your discipline percentage — anything above 70% puts you in the top tier. What habit would you like to improve?";
        if (q.contains("focus") || q.contains("distract")) return "Focus is a skill you train like a muscle. 🧠 Try the 2-minute rule: if a habit takes under 2 minutes, do it now. For longer ones, block time on your calendar as a non-negotiable.";
        if (q.contains("goal") || q.contains("why")) return "Your 'why' is your anchor. 🎯 Write down why each habit matters in 1 sentence. When motivation dips (and it will), that reason pulls you back. Identity beats willpower every time!";
        return "That's a great question! 🤔 Consistency is the real secret to lasting habits — small daily actions compound into extraordinary results over months. What specific habit challenge would you like to tackle today?";
    }

    // ── Request/Response DTOs for OpenRouter ──

    private static class ChatRequest {
        public String model;
        public List<Message> messages;
        public double temperature;

        public ChatRequest(String model, List<Message> messages, double temperature) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
        }
    }

    private static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
