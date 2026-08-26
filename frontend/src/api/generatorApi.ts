/**
 * API client layer for interaction with the CodeClassroom compiler backend.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '' : 'http://localhost:8080');

export interface ProjectGenerationOptions {
  applicationName: string;
  repositoryName: string;
  defaultJavaPackageName: string;
  javaVersion: string;
  databaseType: string;
  authenticationType: string;
  buildTool: string;
}

/**
 * Sends CDL source code to the backend and retrieves the generated project ZIP file as a Blob.
 *
 * @param cdl the CDL source code string
 * @returns a Promise resolving to the Blob representing the downloadable ZIP file
 */
export async function generateProject(cdl: string, options?: ProjectGenerationOptions): Promise<Blob> {
  return postGenerationRequest(`${API_BASE_URL}/api/generate`, cdl, options);
}

export async function generateJavaCode(cdl: string): Promise<Blob> {
  return postGenerationRequest(`${API_BASE_URL}/api/generate/java`, cdl);
}

async function postGenerationRequest(url: string, cdl: string, options?: ProjectGenerationOptions): Promise<Blob> {
  
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ cdl, ...options }),
  });

  if (!response.ok) {
    // ponytail: read and extract descriptive server exception message so users understand why generation failed
    let serverMessage: string | null = null;
    if (typeof response.json === 'function') {
      try {
        const errorJson = await response.json();
        if (errorJson && typeof errorJson.message === 'string') {
          serverMessage = errorJson.message;
        }
      } catch (e) {
        // Ignore JSON parsing/TypeError issues and fallback gracefully
      }
    }
    if (serverMessage) {
      throw new Error(serverMessage);
    }
    throw new Error(`Project generation failed with status code ${response.status}`);
  }

  return await response.blob();
}
