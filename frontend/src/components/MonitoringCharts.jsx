
import { useEffect, useState } from "react";
import {
    ResponsiveContainer,
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
} from "recharts";

import {
    getServerCpuHistory,
    getServerMemoryHistory,
    getServerDiskHistory,
} from "../services/serverService";

function MonitoringCharts({ serverId, serverType = "WINDOWS" }) {
    const [cpuData, setCpuData] = useState([]);
    const [memoryData, setMemoryData] = useState([]);
    const [thirdData, setThirdData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const type = serverType?.toUpperCase();

    const thirdMetric = {
        WINDOWS: {
            title: "Disk Usage History",
            unit: "%",
            color: "#f59e0b",
            status: false,
        },
        TOMCAT: {
            title: "Thread Count History",
            unit: "threads",
            color: "#f59e0b",
            status: false,
        },
        NODEJS: {
            title: "Application Status History",
            unit: "",
            color: "#f59e0b",
            status: true,
        },
    }[type];

    const memoryMetric = {
        WINDOWS: { title: "Memory Usage History", unit: "%" },
        TOMCAT: { title: "JVM Heap Memory History", unit: "MB" },
        NODEJS: { title: "Process Memory History", unit: "MB" },
    }[type];

    useEffect(() => {
        if (!serverId || !thirdMetric || !memoryMetric) {
            setLoading(false);
            return;
        }

        let active = true;

        const fetchHistory = async () => {
            try {
                const [cpu, memory, third] = await Promise.all([
                    getServerCpuHistory(serverId),
                    getServerMemoryHistory(serverId),
                    getServerDiskHistory(serverId),
                ]);

                if (!active) return;

                const formatData = (data) =>
                    data.map((point) => ({
                        time: new Date(
                            point.timestamp * 1000
                        ).toLocaleTimeString(),
                        value: Number(point.value),
                    }));

                setCpuData(formatData(cpu));
                setMemoryData(formatData(memory));
                setThirdData(formatData(third));
                setError("");
            } catch (err) {
                if (active) {
                    setError(err.message || "Failed to load historical data.");
                }
            } finally {
                if (active) setLoading(false);
            }
        };

        setLoading(true);
        fetchHistory();

        const interval = setInterval(fetchHistory, 15000);

        return () => {
            active = false;
            clearInterval(interval);
        };
    }, [serverId, type]);

    const renderChart = (title, data, color, unit, isStatus = false) => (
        <div className="history-card">
            <h3>{title}</h3>

            {data.length === 0 ? (
                <p>No historical data available.</p>
            ) : (
                <ResponsiveContainer width="100%" height={260}>
                    <LineChart data={data}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis dataKey="time" />

                        <YAxis
                            domain={isStatus ? [0, 1] : ["auto", "auto"]}
                            tickFormatter={
                                isStatus
                                    ? (value) => value >= 1 ? "Running" : "Stopped"
                                    : undefined
                            }
                        />

                        <Tooltip
                            formatter={(value) => {
                                const numericValue = Number(value);

                                if (isStatus) {
                                    return numericValue >= 1
                                        ? "Running"
                                        : "Stopped";
                                }

                                return `${numericValue.toFixed(2)} ${unit}`;
                            }}
                        />

                        <Line
                            type={isStatus ? "stepAfter" : "monotone"}
                            dataKey="value"
                            stroke={color}
                            strokeWidth={2}
                            dot={false}
                        />
                    </LineChart>
                </ResponsiveContainer>
            )}
        </div>
    );

    if (loading) return <p>Loading server history...</p>;

    return (
        <section className="monitoring-history">
            <h2>Historical Performance</h2>

            {error && <p className="error-message">{error}</p>}

            {!error && (
                <>
                    {renderChart(
                        "CPU Usage History",
                        cpuData,
                        "#3b82f6",
                        "%"
                    )}

                    {renderChart(
                        memoryMetric.title,
                        memoryData,
                        "#16a34a",
                        memoryMetric.unit
                    )}

                    {renderChart(
                        thirdMetric.title,
                        thirdData,
                        thirdMetric.color,
                        thirdMetric.unit,
                        thirdMetric.status
                    )}

                    <p className="page-description">
                        Historical data covers the last 3 hours and refreshes
                        every 15 seconds.
                    </p>
                </>
            )}
        </section>
    );
}

export default MonitoringCharts;
