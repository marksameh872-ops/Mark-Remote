const WebSocket = require("ws");
const http = require("http");
const PORT = process.env.PORT || 8080;
const server = http.createServer((req, res) => { res.writeHead(200, { "content-type": "text/plain" }); res.end("Mark Remote relay is running"); });
const wss = new WebSocket.Server({ server, maxPayload: 2 * 1024 * 1024 });
const rooms = new Map();
function send(ws, payload) { if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(payload)); }
function leave(ws) { if (!ws.room) return; const room = rooms.get(ws.room); if (room) { room.delete(ws); for (const peer of room) send(peer, { type: "peer-left" }); if (!room.size) rooms.delete(ws.room); } ws.room = null; }
wss.on("connection", ws => {
  ws.on("message", raw => {
    let msg; try { msg = JSON.parse(raw.toString()); } catch { return; }
    if (msg.type === "join") {
      const code = String(msg.code || "").replace(/\\D/g, "").slice(0, 6);
      if (code.length !== 6 || !["host", "viewer"].includes(msg.role)) return send(ws, { type: "error", message: "Invalid code or role" });
      if (msg.role === "host" && rooms.has(code)) return send(ws, { type: "error", message: "Code already in use" });
      if (msg.role === "viewer" && !rooms.has(code)) return send(ws, { type: "error", message: "Session not found" });
      if (!rooms.has(code)) rooms.set(code, new Set());
      const room = rooms.get(code); if (room.size >= 2) return send(ws, { type: "error", message: "Session is full" });
      ws.room = code; ws.role = msg.role; room.add(ws); send(ws, { type: "joined", code, role: msg.role });
      for (const peer of room) if (peer !== ws) send(peer, { type: "peer-joined" }); return;
    }
    if (!ws.room) return; const room = rooms.get(ws.room); if (room) for (const peer of room) if (peer !== ws) send(peer, msg);
  });
  ws.on("close", () => leave(ws)); ws.on("error", () => leave(ws));
});
server.listen(PORT, "0.0.0.0", () => console.log("Mark Remote relay listening on " + PORT));
