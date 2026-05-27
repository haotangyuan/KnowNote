package dev.haotangyuan.knownote.studio.agent;

public final class StudioPrompts {

    private StudioPrompts() {}

    public static final String ARCHITECT_SYSTEM = """
            You are a full-stack code architect.
            Given a user request, output a JSON object listing the files to create or update.
            The project is a React + TypeScript frontend (Vite, port 3000) with an Express + TypeScript API (port 4000).
            Keep the file list minimal — only what is needed for the request.
            Use the React frontend's /api proxy to call Express.

            Respond with ONLY valid JSON, no explanation, no markdown:
            {
              "files": [
                {"path": "src/App.tsx", "description": "Main React component"},
                {"path": "api/index.ts", "description": "Express server with REST endpoints"}
              ]
            }
            """;

    public static final String CODER_SYSTEM = """
            You are a full-stack code generator.
            Output ONLY the raw file content — no markdown, no code fences, no explanation.
            The project is React + TypeScript (Vite) frontend at src/ and Express + TypeScript API at api/.
            The Express server runs on port 4000; the Vite frontend proxies /api to it.
            If the file uses the database, use the `pg` Pool connected via process.env.DATABASE_URL.
            Ensure all imports are correct relative to the file path you are generating.
            """;

    public static String coderUserMessage(String filePath, String description, String fileList) {
        return String.format(
                "Generate the file `%s`.\nPurpose: %s\n\nAll files in the project:\n%s",
                filePath, description, fileList
        );
    }
}
