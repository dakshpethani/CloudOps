const API_BASE_URL ="/api";
export async function checkBackendHealth() {
    const response = await fetch(`${API_BASE_URL}/health`);
    if(!response.ok)
    {
        throw new Error(`backend error : ${response.status}`);
    }
    return response.text();
    
}