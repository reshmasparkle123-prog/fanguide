# FanGuide — AI-Powered Stadium Assistant for FIFA World Cup 2026

## Chosen Vertical
**Multilingual Fan Assistance + Real-Time Navigation & Crowd Management**

FIFA World Cup 2026 will bring fans from over 100 countries into shared stadium
spaces. Language barriers and unpredictable crowd congestion are two of the
most immediate, high-frequency pain points for fans — and both are directly
solvable with Generative AI. FanGuide combines the two into a single,
demo-ready product: a fan-facing assistant that answers questions in the
fan's own language while grounding its answers in live stadium conditions.

## Approach and Logic

1. **Multilingual Q&A (GenAI core)** — Fans select their language and ask
   free-form questions ("Where's the nearest restroom?", "Which gate is
   fastest?"). Requests go to Groq's `llama-3.3-70b-versatile` model with a
   system prompt that (a) locks the response language and (b) injects the
   current live crowd data as context, so answers are grounded rather than
   generic.

2. **Real-time crowd-aware recommendations** — `CrowdService` simulates a
   live density feed across 9 stadium zones (gates, food courts, restrooms).
   Values fluctuate every request to emulate real turnstile/CCTV analytics
   feeding a production system. The AI reads this data and actively steers
   fans away from congested zones — turning a static FAQ bot into a
   real-time decision-support tool.

3. **Accessibility** — Beyond the one-tap font-size/spacing toggle, the UI
   follows WCAG-aligned practices: semantic landmarks (`<main>`, `<section>`),
   a skip-to-content link, labelled form controls, `aria-live` regions so
   screen readers announce new chat replies and crowd updates automatically,
   visible focus outlines for keyboard navigation, and `aria-label`s on all
   icon-only controls.

4. **Graceful degradation** — If the Groq API is unreachable (rate limits,
   network issues during a live demo), `GroqService` falls back to a
   keyword-matching responder so the assistant never goes silent — this
   is a deliberate reliability/testing decision, not a missing feature.

## How the Solution Works (Architecture)

```
Fan's browser (index.html)
     │  language + question
     ▼
Spring Boot REST API (/api/chat, /api/crowd)
     │
     ├── CrowdService  → simulated live density per zone
     └── GroqService   → Groq LLM call, grounded with crowd context
                        → falls back to rule-based answer on failure
```

- **Backend:** Java 17, Spring Boot 3.3, deployed on Railway
- **AI:** Groq API (`llama-3.3-70b-versatile`)
- **Frontend:** Single-page vanilla HTML/CSS/JS (no build step — keeps repo
  small and deploy simple, matching the <10MB submission constraint)
- **Data:** Simulated in-memory crowd feed (see Assumptions)

## Assumptions

- Real stadium turnstile/CCTV crowd data is not available for this hackathon,
  so `CrowdService` generates realistic, fluctuating mock density values per
  zone. In production this would be replaced with a live sensor/camera feed
  — the AI reasoning layer would not need to change.
- Zone names (gates, food courts, restrooms) are illustrative placeholders
  representative of a typical World Cup host stadium layout.
- The environment variable `GROQ_API_KEY` must be set at runtime (Railway
  dashboard or local `export`) — it is never hardcoded or committed.
- Language list is a representative subset of major FIFA 2026 fan
  nationalities (English, Hindi, Spanish, Portuguese, French, Arabic,
  Mandarin, Kannada); the model can generalize beyond this list since
  language is passed as free text to the prompt.

## Testing

Unit and integration tests cover the core logic:

- `CrowdServiceTest` — validates all 9 zones are returned, density values
  stay within bounds across repeated calls, and status labels (LOW/MODERATE/
  HIGH) are generated correctly.
- `GroqServiceTest` — validates the fallback responder (used whenever
  `GROQ_API_KEY` is unset or the API is unreachable) never returns a blank
  answer and stays on-topic for restroom/gate/food questions.
- `FanGuideControllerTest` — Spring `MockMvc` integration tests for
  `/api/health`, `/api/crowd`, and `/api/chat`, including the default-language
  fallback when a client omits `language`.

Run with:
```bash
mvn test
```

## Running Locally

```bash
export GROQ_API_KEY=your_key_here
mvn clean package
java -jar target/fanguide-1.0.0.jar
```

Visit `http://localhost:8080`.

## Deployment

Deployed on Railway (Nixpacks build). Set `GROQ_API_KEY` as an environment
variable in the Railway project settings — it is re-injected at build/deploy
time and never stored in the repo.
