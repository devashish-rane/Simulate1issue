import React, { useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  ClipboardList,
  Clock3,
  Database,
  KeyRound,
  Loader2,
  Play,
  RefreshCcw,
  Search,
  Send,
  ShieldCheck,
  TimerReset,
  XCircle
} from "lucide-react";
import "./styles.css";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

const INCIDENTS = [
  {
    id: "inc-01",
    title: "Green health, one business action fails",
    method: "GET",
    path: "/api/lab/feature",
    symptom: "Health check is UP, but this action returns a server error.",
    capture: "status code, API request ID, consumer log line"
  },
  {
    id: "inc-02",
    title: "Intermittent 503 after repeated calls",
    method: "GET",
    path: "/api/lab/inventory",
    repeats: 4,
    symptom: "Several calls succeed, then one fails with a service-style error.",
    capture: "which attempt failed, request ID, target 5xx metric"
  },
  {
    id: "inc-03",
    title: "Tail latency spike",
    method: "GET",
    path: "/api/lab/profile",
    repeats: 10,
    symptom: "Most calls are fast, but one call may be much slower.",
    capture: "slowest request, p90/p99 graph, trace ID"
  },
  {
    id: "inc-04",
    title: "CPU rises after one action",
    method: "GET",
    path: "/api/lab/aggregate?durationMs=3500",
    parallel: 3,
    symptom: "Latency increases and ECS CPU should move after the trigger.",
    capture: "latency, ECS CPU graph, API p99 graph"
  },
  {
    id: "inc-05",
    title: "Memory trend changes",
    method: "GET",
    path: "/api/lab/cache?mb=3",
    repeats: 5,
    symptom: "Repeated calls report increasing memory-related values.",
    capture: "response body, ECS memory graph, task count"
  },
  {
    id: "inc-06",
    title: "Concurrent requests slow each other",
    method: "GET",
    path: "/api/lab/reconcile",
    parallel: 3,
    symptom: "Parallel calls complete slowly instead of all finishing at once.",
    capture: "per-call latency, p99 graph, overlapping timestamps"
  },
  {
    id: "inc-07",
    title: "Large response is slow",
    method: "GET",
    path: "/api/lab/export?kb=7000",
    symptom: "The action returns a large body and takes longer than normal.",
    capture: "latency, response size, API Gateway latency"
  },
  {
    id: "inc-08",
    title: "Expected 401 path",
    method: "GET",
    path: "/api/lab/session",
    symptom: "The call is rejected with an authorization-style response.",
    capture: "status code, response body, whether it should page"
  },
  {
    id: "inc-09",
    title: "Expected 4xx path",
    method: "GET",
    path: "/api/lab/quote?plan=starter&items=1",
    symptom: "The call fails as a client-side/business validation response.",
    capture: "status code, API 4xx metric, error shape"
  },
  {
    id: "inc-10",
    title: "Gateway timeout",
    method: "GET",
    path: "/api/lab/timeout",
    symptom: "The request waits and eventually fails at the edge.",
    capture: "elapsed time, API Gateway logs, app logs around same timestamp"
  },
  {
    id: "inc-11",
    title: "Job create is rejected",
    kind: "badTokenJob",
    symptom: "Job creation returns an authorization failure.",
    capture: "status code, API request ID, consumer auth log"
  },
  {
    id: "inc-12",
    title: "Job create missing credentials",
    kind: "noAuthJob",
    symptom: "Job creation fails before the worker gets involved.",
    capture: "status code, response body, absence of SQS activity"
  },
  {
    id: "inc-13",
    title: "Job stays unfinished at first",
    kind: "job",
    body: { jobType: "slow", slow: true, durationMs: 10000 },
    symptom: "The job is accepted but does not complete immediately.",
    capture: "job ID, first status poll, SQS visible/age metrics"
  },
  {
    id: "inc-14",
    title: "Job changes state after retries",
    kind: "job",
    body: { jobType: "retry", fail: true },
    symptom: "The job is accepted, then later shows retry/final status movement.",
    capture: "job ID, attempt count, worker logs over time"
  },
  {
    id: "inc-15",
    title: "Failure queue exercise",
    kind: "job",
    body: { jobType: "poison", poison: true },
    symptom: "The job is accepted but should eventually stop progressing normally.",
    capture: "job ID, DLQ visible count, worker warning logs"
  },
  {
    id: "inc-16",
    title: "Queue backlog exercise",
    kind: "backlog",
    body: { jobType: "slow", slow: true, durationMs: 10000 },
    count: 8,
    symptom: "Many jobs are accepted quickly, then completion lags behind.",
    capture: "job IDs, SQS visible messages, oldest message age"
  },
  {
    id: "inc-17",
    title: "Token request rejected",
    kind: "badCredentials",
    symptom: "Token creation is rejected before consumer-service is called.",
    capture: "status code, auth-service logs, API access log"
  },
  {
    id: "inc-18",
    title: "Success status with failed body",
    method: "GET",
    path: "/api/lab/mismatch",
    symptom: "HTTP status looks successful, but the body says the operation failed.",
    capture: "HTTP status, response body, dashboard success count"
  },
  {
    id: "inc-19",
    title: "Alternating checkout failure",
    method: "GET",
    path: "/api/lab/checkout",
    repeats: 2,
    symptom: "A repeated business action gives different results within seconds.",
    capture: "both request IDs, status codes, consumer error log"
  },
  {
    id: "inc-20",
    title: "Route not found",
    method: "GET",
    path: "/api/lab/not-a-real-route",
    symptom: "The request reaches the system but no application route handles it.",
    capture: "status code, API route, ALB target response"
  }
];

const JOB_TEMPLATES = [
  {
    id: "normal",
    label: "Normal",
    body: { jobType: "demo" },
    icon: CheckCircle2
  },
  {
    id: "slow",
    label: "Slow",
    body: { jobType: "slow", slow: true, durationMs: 10000 },
    icon: Clock3
  },
  {
    id: "retry",
    label: "Retry",
    body: { jobType: "retry", fail: true },
    icon: RefreshCcw
  },
  {
    id: "poison",
    label: "Poison",
    body: { jobType: "poison", poison: true },
    icon: AlertTriangle
  }
];

function App() {
  const [clientId, setClientId] = useState("demo-client");
  const [clientSecret, setClientSecret] = useState("demo-secret");
  const [token, setToken] = useState("");
  const [tokenMeta, setTokenMeta] = useState(null);
  const [selectedJob, setSelectedJob] = useState("normal");
  const [jobId, setJobId] = useState("");
  const [jobStatus, setJobStatus] = useState(null);
  const [lastResponse, setLastResponse] = useState(null);
  const [history, setHistory] = useState([]);
  const [busy, setBusy] = useState({});

  const selectedTemplate = useMemo(
    () => JOB_TEMPLATES.find((template) => template.id === selectedJob) || JOB_TEMPLATES[0],
    [selectedJob]
  );

  async function request(path, options = {}) {
    const method = options.method || "GET";
    const started = performance.now();
    const url = `${API_BASE_URL}${path}`;
    const response = await fetch(url, {
      ...options,
      headers: {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {})
      }
    });
    const latencyMs = Math.round(performance.now() - started);
    const contentType = response.headers.get("content-type") || "";
    const rawBody = await response.text();
    const parsedBody = parseBody(rawBody, contentType);
    const result = {
      id: crypto.randomUUID(),
      method,
      path,
      status: response.status,
      ok: response.ok,
      latencyMs,
      body: parsedBody,
      requestId: response.headers.get("x-amzn-requestid") || response.headers.get("x-amzn-RequestId"),
      traceId: response.headers.get("x-amzn-trace-id"),
      time: new Date().toLocaleTimeString()
    };

    setLastResponse(result);
    setHistory((items) => [result, ...items].slice(0, 10));

    if (!response.ok) {
      const error = new Error(`HTTP ${response.status}`);
      error.result = result;
      throw error;
    }

    return result;
  }

  async function withBusy(key, action) {
    setBusy((state) => ({ ...state, [key]: true }));
    try {
      return await action();
    } finally {
      setBusy((state) => ({ ...state, [key]: false }));
    }
  }

  async function checkHealth() {
    await withBusy("health", () => request("/actuator/health"));
  }

  async function issueToken() {
    return withBusy("token", async () => {
      const result = await request("/auth/token", {
        method: "POST",
        body: JSON.stringify({ clientId, clientSecret })
      });
      setToken(result.body.accessToken || "");
      setTokenMeta(result.body);
      return result.body.accessToken;
    });
  }

  async function ensureToken() {
    if (token) {
      return token;
    }
    return issueToken();
  }

  async function createJob() {
    await withBusy("job", async () => {
      const accessToken = await ensureToken();
      const result = await request("/api/jobs", {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}` },
        body: JSON.stringify(selectedTemplate.body)
      });
      setJobId(result.body.jobId || "");
      setJobStatus(result.body);
    });
  }

  async function pollJob(nextJobId = jobId) {
    if (!nextJobId) {
      return;
    }

    await withBusy("poll", async () => {
      const accessToken = await ensureToken();
      const result = await request(`/api/jobs/${nextJobId}`, {
        headers: { Authorization: `Bearer ${accessToken}` }
      });
      setJobStatus(result.body);
    });
  }

  async function safeRequest(path, options = {}) {
    try {
      return await request(path, options);
    } catch (error) {
      if (error.result) {
        return error.result;
      }
      throw error;
    }
  }

  async function runIncident(incident) {
    await withBusy(incident.id, async () => {
      if (incident.kind === "job") {
        const accessToken = await ensureToken();
        const result = await safeRequest("/api/jobs", {
          method: "POST",
          headers: { Authorization: `Bearer ${accessToken}` },
          body: JSON.stringify(incident.body)
        });
        if (result.body?.jobId) {
          setJobId(result.body.jobId);
          setJobStatus(result.body);
        }
        return;
      }

      if (incident.kind === "backlog") {
        const accessToken = await ensureToken();
        const calls = Array.from({ length: incident.count }, () =>
          safeRequest("/api/jobs", {
            method: "POST",
            headers: { Authorization: `Bearer ${accessToken}` },
            body: JSON.stringify(incident.body)
          })
        );
        const results = await Promise.all(calls);
        const firstJob = results.find((result) => result.body?.jobId);
        if (firstJob) {
          setJobId(firstJob.body.jobId);
          setJobStatus(firstJob.body);
        }
        return;
      }

      if (incident.kind === "badTokenJob") {
        await safeRequest("/api/jobs", {
          method: "POST",
          headers: { Authorization: "Bearer bad-token" },
          body: JSON.stringify({ jobType: "invalid-token" })
        });
        return;
      }

      if (incident.kind === "noAuthJob") {
        await safeRequest("/api/jobs", {
          method: "POST",
          body: JSON.stringify({ jobType: "missing-auth" })
        });
        return;
      }

      if (incident.kind === "badCredentials") {
        await safeRequest("/auth/token", {
          method: "POST",
          body: JSON.stringify({ clientId, clientSecret: "wrong-secret" })
        });
        return;
      }

      const options = { method: incident.method };
      if (incident.parallel) {
        await Promise.all(Array.from({ length: incident.parallel }, () => safeRequest(incident.path, options)));
        return;
      }

      const repeats = incident.repeats || 1;
      for (let index = 0; index < repeats; index += 1) {
        await safeRequest(incident.path, options);
      }
    });
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Prod Core</p>
          <h1>On-call learning console</h1>
        </div>
        <div className="topbar-actions">
          <button className="secondary" onClick={checkHealth} disabled={busy.health}>
            <ButtonIcon loading={busy.health} icon={Activity} />
            Health
          </button>
          <span className={token ? "status-pill good" : "status-pill neutral"}>
            {token ? "Token ready" : "No token"}
          </span>
        </div>
      </header>

      <section className="grid two">
        <section className="panel">
          <div className="panel-title">
            <Search size={20} />
            <div>
              <h2>Last response</h2>
              <p>Use request IDs and trace IDs to jump into AWS investigations.</p>
            </div>
          </div>
          <ResponseView response={lastResponse} />
        </section>

        <section className="panel">
          <div className="panel-heading-row">
            <div className="panel-title">
              <TimerReset size={20} />
              <div>
                <h2>Recent calls</h2>
                <p>Fast history for reproducing symptoms.</p>
              </div>
            </div>
            <button className="secondary compact" onClick={() => setHistory([])} disabled={history.length === 0}>
              Clear
            </button>
          </div>
          <div className="history">
            {history.length === 0 && <p className="muted">No calls yet.</p>}
            {history.map((item) => (
              <button
                className={lastResponse?.id === item.id ? "history-row selected" : "history-row"}
                key={item.id}
                onClick={() => setLastResponse(item)}
                type="button"
              >
                <span className={item.ok ? "dot good" : "dot bad"} />
                <span className="mono">{item.method}</span>
                <span>{item.path}</span>
                <strong>{item.status}</strong>
                <span>{item.latencyMs} ms</span>
              </button>
            ))}
          </div>
        </section>
      </section>

      <section className="panel wide">
        <div className="panel-title">
          <ClipboardList size={20} />
          <div>
            <h2>Incident launcher</h2>
            <p>Trigger a symptom, collect evidence, and find the failing layer without the UI giving away the cause.</p>
          </div>
        </div>
        <div className="drill-grid">
          {INCIDENTS.map((incident) => (
            <button
              key={incident.id}
              className="drill-card"
              onClick={() => runIncident(incident)}
              disabled={busy[incident.id]}
            >
              <span className="drill-icon">
                <ButtonIcon loading={busy[incident.id]} icon={Play} />
              </span>
              <span>
                <strong>{incident.id.toUpperCase()}: {incident.title}</strong>
                <small>{triggerLabel(incident)}</small>
                <em>{incident.symptom}</em>
                <b>{incident.capture}</b>
              </span>
            </button>
          ))}
        </div>
      </section>

      <section className="grid two">
        <section className="panel">
          <div className="panel-title">
            <ShieldCheck size={20} />
            <div>
              <h2>Auth token provider</h2>
              <p>Issue a short-lived token for manual consumer calls.</p>
            </div>
          </div>
          <div className="form-grid">
            <label>
              Client ID
              <input value={clientId} onChange={(event) => setClientId(event.target.value)} />
            </label>
            <label>
              Client Secret
              <input
                type="password"
                value={clientSecret}
                onChange={(event) => setClientSecret(event.target.value)}
              />
            </label>
          </div>
          <button className="primary" onClick={issueToken} disabled={busy.token}>
            <ButtonIcon loading={busy.token} icon={KeyRound} />
            Issue token
          </button>
          <dl className="compact-list">
            <div>
              <dt>Expires</dt>
              <dd>{tokenMeta?.expiresAt || "not issued"}</dd>
            </div>
            <div>
              <dt>Token</dt>
              <dd className="mono">{token ? `${token.slice(0, 12)}...${token.slice(-6)}` : "empty"}</dd>
            </div>
          </dl>
        </section>

        <section className="panel">
          <div className="panel-title">
            <Database size={20} />
            <div>
              <h2>Manual async job tools</h2>
              <p>Optional controls for directly testing consumer, SQS, worker, and DynamoDB state.</p>
            </div>
          </div>
          <div className="segmented">
            {JOB_TEMPLATES.map((template) => {
              const Icon = template.icon;
              return (
                <button
                  key={template.id}
                  className={selectedJob === template.id ? "selected" : ""}
                  onClick={() => setSelectedJob(template.id)}
                >
                  <Icon size={15} />
                  {template.label}
                </button>
              );
            })}
          </div>
          <pre className="payload">{JSON.stringify(selectedTemplate.body, null, 2)}</pre>
          <div className="button-row">
            <button className="primary" onClick={createJob} disabled={busy.job}>
              <ButtonIcon loading={busy.job} icon={Send} />
              Create job
            </button>
            <button className="secondary" onClick={() => pollJob()} disabled={!jobId || busy.poll}>
              <ButtonIcon loading={busy.poll} icon={RefreshCcw} />
              Poll status
            </button>
          </div>
          <JobStatus job={jobStatus} />
        </section>
      </section>
    </main>
  );
}

function ButtonIcon({ loading, icon: Icon }) {
  if (loading) {
    return <Loader2 className="spin" size={16} />;
  }
  return <Icon size={16} />;
}

function triggerLabel(incident) {
  if (incident.kind === "job") {
    return "POST /api/jobs";
  }
  if (incident.kind === "backlog") {
    return `${incident.count}x POST /api/jobs`;
  }
  if (incident.kind === "badTokenJob" || incident.kind === "noAuthJob") {
    return "POST /api/jobs";
  }
  if (incident.kind === "badCredentials") {
    return "POST /auth/token";
  }
  const prefix = incident.parallel ? `${incident.parallel}x parallel ` : incident.repeats ? `${incident.repeats}x ` : "";
  return `${prefix}${incident.method} ${incident.path}`;
}

function JobStatus({ job }) {
  if (!job) {
    return <p className="muted">No job created yet.</p>;
  }

  const status = job.status || "UNKNOWN";
  const ok = status === "SUCCEEDED";
  const failed = status === "FAILED";

  return (
    <div className="job-status">
      {ok ? <CheckCircle2 size={20} /> : failed ? <XCircle size={20} /> : <Clock3 size={20} />}
      <div>
        <strong>{status}</strong>
        <span className="mono">{job.jobId}</span>
        <small>{job.lastError || `type=${job.jobType || "demo"} attempt=${job.attempt ?? "n/a"}`}</small>
      </div>
    </div>
  );
}

function ResponseView({ response }) {
  if (!response) {
    return <p className="muted">Run a health check, auth call, job, or drill.</p>;
  }

  return (
    <div className="response-view">
      <div className="response-meta">
        <span className={response.ok ? "status-pill good" : "status-pill bad"}>
          HTTP {response.status}
        </span>
        <span>{response.latencyMs} ms</span>
        <span className="mono">{response.method} {response.path}</span>
      </div>
      <dl className="compact-list">
        <div>
          <dt>API request ID</dt>
          <dd className="mono">{response.requestId || "not exposed"}</dd>
        </div>
        <div>
          <dt>X-Ray trace ID</dt>
          <dd className="mono">{response.traceId || "not exposed"}</dd>
        </div>
      </dl>
      <pre className="payload">{formatBody(response.body)}</pre>
    </div>
  );
}

function parseBody(rawBody, contentType) {
  if (!rawBody) {
    return null;
  }
  if (contentType.includes("application/json")) {
    try {
      return JSON.parse(rawBody);
    } catch {
      return rawBody;
    }
  }
  return rawBody.length > 4000 ? `${rawBody.slice(0, 4000)}\n...truncated` : rawBody;
}

function formatBody(body) {
  if (body == null) {
    return "null";
  }
  if (typeof body === "string") {
    return body;
  }
  return JSON.stringify(body, null, 2);
}

createRoot(document.getElementById("root")).render(<App />);
