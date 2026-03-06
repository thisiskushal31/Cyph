# Cyph – Compliance (SOC 2, ISO 27001, GDPR, India DPDP)

This document describes how Cyph is designed to support common international and regional compliance frameworks. **Implementing these controls and maintaining evidence is the organization’s responsibility** when deploying Cyph.

---

## 1. SOC 2 (Trust Services Criteria)

Cyph can support SOC 2 alignment as follows.

| Criterion | Cyph controls |
|----------|----------------|
| **Security** | Access control (admin vs user; extension JWT); encryption of secrets at rest (AES-256-GCM); TLS in transit; audit logging (login, credential push/reveal, user/group changes); secure session and token handling. |
| **Availability** | Deployment on reliable infrastructure; DB backups; no single hardcoded dependency on external SaaS for core flows. |
| **Confidentiality** | Credentials and messages encrypted at rest; access only for assigned users; audit of credential reveal. |
| **Processing integrity** | Validated inputs; transactional persistence; audit trail for credential and user operations. |
| **Privacy** | See §4 (GDPR) and §5 (India). Minimize PII in logs; support data subject rights via admin and data handling procedures. |

**Evidence to maintain:** Key management, access reviews, audit log retention, backup/restore tests, incident response playbooks.

---

## 2. ISO 27001 (Information Security Management)

Relevant controls and how Cyph supports them:

| Area | Cyph support |
|------|--------------|
| **A.9 Access control** | Role-based access (admin vs user); extension token scoped to authenticated user; credential access only for assigned users/groups or owner (personal). |
| **A.10 Cryptography** | AES-256-GCM for credential and message payloads; TLS for transport; password hashing (e.g. bcrypt) for form login. |
| **A.12 Operations security** | Audit logging; secure configuration via env (no default secrets in prod); logging of security-relevant events (login, extension login, credential reveal, push, revoke). |
| **A.12.4 Logging** | Audit log for credential and user/group events; optional integration with SIEM. |
| **A.14 System acquisition, development, maintenance** | Secure development practices; dependency management; no hardcoded credentials. |

Key management (e.g. encryption keys for credentials) should be documented and rotated according to organizational policy.

---

## 3. International standards (high level)

- **Encryption:** Secrets (credentials, message payloads) encrypted at rest; TLS in transit.
- **Access control:** Authentication (session + extension JWT); authorization by user/group and ownership.
- **Audit:** Events logged for credential push, revoke, reveal, and user/group changes; support for retention and review.
- **Availability / resilience:** Deployment and backup strategy are organization-defined; Cyph does not mandate a specific SLA.

---

## 4. GDPR (EU / EEA)

| Aspect | Cyph support |
|--------|--------------|
| **Lawful basis** | Organization determines lawful basis (e.g. contract, legitimate interest); Cyph processes data as instructed by the deploying organization (data controller). |
| **Data minimization** | Only necessary data stored (e.g. email, credentials metadata, audit events); audit log can be configured to avoid storing unnecessary PII. |
| **Encryption** | Personal data in credentials and messages encrypted at rest; TLS in transit. |
| **Rights (access, rectification, erasure, etc.)** | Admin can list/update/remove users and their data; credential data can be deleted (revoke shared, delete personal); procedures for export/erasure should be defined by the organization. |
| **Breach notification** | Organization is responsible; audit log supports detection and investigation. |
| **DPO / records of processing** | Organization maintains records; Cyph design (data stored, purposes, retention) can be documented from this file and PRODUCT.md. |

**Note:** The deploying organization is the data controller; Cyph operates as part of the organization’s processing. Data processing agreements and privacy notices are the organization’s responsibility.

---

## 5. India – Data protection (DPDP Act 2023)

| Aspect | Cyph support |
|--------|--------------|
| **Consent / lawful use** | Organization ensures valid consent or other lawful basis; Cyph stores and processes data as configured by the organization. |
| **Purpose limitation** | Credentials and messages used for secret sharing and password management as configured; no secondary use by Cyph itself. |
| **Data minimization** | Only data necessary for the service (user identity, credentials, audit) is stored. |
| **Security** | Encryption at rest and in transit; access control; audit logging. |
| **Rights (access, correction, erasure, etc.)** | Admin can manage users and credentials; organization can define procedures for data subject requests. |
| **Data localisation** | Organization can deploy Cyph (backend and DB) within India or in a jurisdiction of choice and document in its privacy/compliance posture. |

**Note:** Compliance with the Digital Personal Data Protection Act 2023 (DPDP) and related rules is the responsibility of the data fiduciary (the organization deploying Cyph). This document only describes technical controls that can support such compliance.

---

## 6. Summary

- **SOC 2 / ISO 27001:** Encryption, access control, audit logging, and secure configuration support security and confidentiality objectives; the organization maintains evidence and procedures.
- **GDPR:** Encryption, minimization, and admin-controlled user/credential management support compliance; the organization fulfils controller obligations (lawful basis, rights, breach, DPO).
- **India DPDP:** Same technical controls (security, minimization, rights support) apply; the organization ensures lawful basis and data localisation as required.

For deployment and security details, see **DEPLOYMENT.md** and **PRODUCT.md**.
