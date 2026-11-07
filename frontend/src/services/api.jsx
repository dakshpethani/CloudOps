
import { apiUrl } from "./apiConfig";

export async function checkBackendHealth() {
    const response = await fetch(apiUrl("/api/health"));

    if (!response.ok) {
        throw new Error(`backend error : ${response.status}`);
    }

    return response.text();
}
