import { useState } from "react";
import "./App.css";

const API_URL =
  "https://payment-transaction-service-production-44e3.up.railway.app";

function App() {
  const [senderId, setSenderId] = useState("");
  const [recipientId, setRecipientId] = useState("");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const sendPayment = async (e) => {
    e.preventDefault();

    setMessage("");
    setError("");
    setLoading(true);

    try {
      const response = await fetch(`${API_URL}/api/transactions/send`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          senderId: Number(senderId),
          recipientId: Number(recipientId),
          amount: Number(amount),
          description: description,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || "Payment failed");
      }

      setMessage(
        `Payment successful! Transaction status: ${
          data.status || "COMPLETED"
        }`
      );

      setSenderId("");
      setRecipientId("");
      setAmount("");
      setDescription("");
    } catch (err) {
      setError(err.message || "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app">
      <div className="payment-card">
        <div className="icon">💳</div>

        <h1>Payment Transaction</h1>
        <p className="subtitle">Secure Payment Service</p>

        <form onSubmit={sendPayment}>
          <label>Sender ID</label>
          <input
            type="number"
            value={senderId}
            onChange={(e) => setSenderId(e.target.value)}
            placeholder="Enter sender ID"
            required
          />

          <label>Recipient ID</label>
          <input
            type="number"
            value={recipientId}
            onChange={(e) => setRecipientId(e.target.value)}
            placeholder="Enter recipient ID"
            required
          />

          <label>Amount</label>
          <input
            type="number"
            min="1"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="Enter amount"
            required
          />

          <label>Description</label>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Payment description"
          />

          <button type="submit" disabled={loading}>
            {loading ? "Processing..." : "Send Payment"}
          </button>
        </form>

        {message && <div className="success">{message}</div>}

        {error && <div className="error">{error}</div>}

        <div className="footer">
          Powered by Spring Boot + React
        </div>
      </div>
    </div>
  );
}

export default App;