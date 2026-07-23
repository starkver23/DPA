/**
 * API client layer for interaction with the CodeClassroom compiler backend.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '' : 'http://localhost:8080');

/**
 * Sends CDL source code to the backend and retrieves the generated project ZIP file as a Blob.
 *
 * @param cdl the CDL source code string
 * @returns a Promise resolving to the Blob representing the downloadable ZIP file
 */
export async function generateProject(cdl: string): Promise<Blob> {
  const url = `${API_BASE_URL}/api/generate`;
  
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ cdl }),
  });

  if (!response.ok) {
    throw new Error(`Project generation failed with status code ${response.status}`);
  }

  return await response.blob();
}
