# Mémo d'Étude : GestionnaireCrypto.java

Ce document sert de support de révision pour le fichier `GestionnaireCrypto.java`. Cette classe est le cœur de la sécurité de l'application, centralisant toutes les primitives cryptographiques.

## 1. Vue d'ensemble et Algorithmes

La classe utilise le provider **BouncyCastle** (`Security.addProvider`) pour accéder à une suite étendue d'algorithmes.

| Type | Algorithme | Taille de Clé | Usage |
| :--- | :--- | :--- | :--- |
| **Symétrique** | `AES` | 256 bits | Chiffrement rapide des données de session (gros volumes). |
| **Asymétrique** | `RSA` | 2048 bits | Échange de clés ou chiffrement de petites données, Signature numérique. |
| **Hachage** | `SHA-256` | N/A | Empreinte numérique (Digest), utilisé avec sel pour les mots de passe. |
| **MAC** | `HmacSHA256` | N/A | Authenticité et Intégrité des messages (nécessite une clé secrète). |
| **Signature** | `SHA256withRSA`| N/A | Non-répudiation et Authenticité (Signer avec Privée, Vérifier avec Publique). |

---

## 2. Analyse Détaillée des Méthodes

### A. Génération de Clés et Hachage
*   **`genererCleSession()`** : Crée une clé AES temporaire pour la session en cours.
    *   *Note* : Utilise `KeyGenerator` (pour symétrique).
*   **`genererPaireClesRSA()`** : Crée une paire (Publique / Privée).
    *   *Note* : Utilise `KeyPairGenerator` (pour asymétrique).
*   **`genererSel(int taille)`** : Génère des octets aléatoires via `SecureRandom`.
    *   *Usage* : Indispensable pour contrer les attaques par dictionnaire/rainbow tables sur les mots de passe.
*   **`calculerDigestSale(...)`** :
    *   Combine `login` + `motDePasse` + `sel`.
    *   Utilise `MessageDigest`.
    *   C'est une fonction à sens unique (on ne peut pas retrouver le mot de passe depuis le digest).

### B. Chiffrement (Confidentialité)
*   **Symétrique (`chiffrerAES` / `dechiffrerAES`)** :
    *   Utilise la **même clé** (`SecretKey`) pour chiffrer et déchiffrer.
    *   Mode `Cipher.ENCRYPT_MODE` vs `Cipher.DECRYPT_MODE`.
*   **Asymétrique (`chiffrerRSA` / `dechiffrerRSA`)** :
    *   **Chiffrement** : Avec la **Clé Publique** du destinataire (tout le monde peut chiffrer pour lui).
    *   **Déchiffrement** : Avec la **Clé Privée** du destinataire (seul lui peut lire).

### C. Signature Numérique (Authenticité, Intégrité, Non-répudiation)
*   **`signer(..., PrivateKey)`** :
    *   L'émetteur utilise sa **Clé Privée**.
    *   Permet de prouver que c'est bien lui qui a émis le message.
*   **`verifierSignature(..., PublicKey)`** :
    *   Le récepteur utilise la **Clé Publique** de l'émetteur.
    *   Si `verify` retourne `true`, le message vient bien de l'émetteur et n'a pas été modifié.

### D. HMAC (Intégrité et Authenticité Symétrique)
*   **`calculerHMAC` / `verifierHMAC`** :
    *   Utilise une clé secrète partagée (contrairement à la signature qui utilise des clés asymétriques).
    *   Plus rapide que la signature RSA.
    *   Garantit que le message n'a pas été altéré en transit (`MessageDigest.isEqual`).

### E. Utilitaires (Base64)
*   Les méthodes `...VersBase64` et `base64Vers...` servent au **transport** et au **stockage**.
*   Une clé est binaire (`byte[]`), illisible et difficile à copier-coller ou stocker en JSON/Texte. Le Base64 la transforme en String ASCII sûr.
*   *Classes Spécifiques Java* :
    *   `X509EncodedKeySpec` -> Pour **Clé Publique**.
    *   `PKCS8EncodedKeySpec` -> Pour **Clé Privée**.
    *   `SecretKeySpec` -> Pour **Clé Secrète** (AES).

---

## 3. Points Clés pour l'Examen

1.  **Différence `Cipher` vs `Signature`** :
    *   `Cipher` (RSA) : Public -> Chiffre, Privé -> Déchiffre. (Pour le secret).
    *   `Signature` (RSA) : Privé -> Signe, Public -> Vérifie. (Pour l'authenticité).
2.  **Pourquoi le Sel (Salt) ?**
    *   Si deux utilisateurs ont le même mot de passe "123456", sans sel, ils auraient le même hash. Avec un sel aléatoire unique par utilisateur, les hashs sont différents. Empêche les attaques pré-calculées.
3.  **Pourquoi BouncyCastle ?**
    *   Fournit des implémentations cryptographiques plus robustes ou plus récentes que le JDK standard par défaut.
4.  **AES vs RSA** :
    *   On utilise RSA pour échanger la clé AES (Handshake), puis AES pour chiffrer toute la communication (car AES est 1000x plus rapide).
