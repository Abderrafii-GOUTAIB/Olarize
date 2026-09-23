# Knowledge and RAG instructions

Own extraction, chunks, embeddings, index jobs and retrieval. The documents module owns original-file metadata, access grants, versions and publication authority. PostgreSQL/pgvector is the initial search store; Bedrock provides embedding inference through Spring AI.

## Ingestion lifecycle

Authorized upload -> private S3 -> file validation and malware controls -> text extraction -> HR review -> chunking and embedding -> atomic activation of the approved index version. Keep review approval and index readiness as distinct states. Use Apache Tika/PDFBox for supported text-based files. Scanned or unsupported documents must show a clear status; do not pretend extraction succeeded.

Store source document/version, page or section locator, company, language, effective dates, publication state, content hash, embedding model and dimensions with chunks. Use the same embedding model for questions and passages. Re-embedding requires a versioned migration strategy. Failed ingestion must not displace a usable approved index.

## Retrieval security and quality

Retrieve only permitted and applicable content. Apply company and access constraints in the search layer and recheck publication/access before building model context. Never send unauthorized passages to the model and filter afterward. Derive scope from authentication. A public legal corpus, if added, must be explicitly distinguished from private tenant knowledge.

Respect revocation and deletion immediately at access checks, and propagate changes to indexes and caches. A stale chunk is not a grant of access. Keep sensitive individual personnel files outside the general policy corpus.

Begin with vector retrieval. Add keyword fusion or reranking only after a reviewed evaluation demonstrates benefit. Resolve citations to authorized documents and actual source locations. If evidence is absent or conflicts, explain that rather than fabricate a rule. Test cross-company search, future/superseded policies, failed jobs, revoked access and citation correctness.

## V1 scope distinction

Read-only operational AI tools may ship before RAG. Attendance, payroll inputs and CNSS/CIMR totals come from authorized services, not vector retrieval. RAG may explain reviewed source texts but never activates contribution rules or supplies missing numeric values.
