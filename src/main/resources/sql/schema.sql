CREATE TABLE IF NOT EXISTS products
(
    product_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(250)                          NOT NULL,
    category    VARCHAR(100)                          NOT NULL,
    description VARCHAR(500)                          NOT NULL,
    price       DECIMAL(10, 2)                        NOT NULL,
    popularity  INT                                   NOT NULL,
    image_url   VARCHAR(500),
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  VARCHAR(20)                           NOT NULL,
    updated_at  TIMESTAMP   DEFAULT NULL,
    updated_by  VARCHAR(20) DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS categories (
    category_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE COMMENT 'Code unique (SPORTS, ANIME, etc.)',
    name          VARCHAR(100) NOT NULL COMMENT 'Nom de la catégorie',
    description   VARCHAR(500) COMMENT 'Description',
    icon          VARCHAR(10) COMMENT 'Emoji icon',
    display_order INT DEFAULT 0 COMMENT 'Ordre d''affichage',
    is_active     BOOLEAN DEFAULT TRUE COMMENT 'Actif/Inactif',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_at    TIMESTAMP DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    updated_by    VARCHAR(100) DEFAULT NULL,

    INDEX idx_code (code),
    INDEX idx_active (is_active),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =====================================================
-- TABLE POUR LA GALERIE D'IMAGES DES PRODUITS
-- =====================================================

CREATE TABLE IF NOT EXISTS product_gallery_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_display_order (display_order)
)

CREATE TABLE IF NOT EXISTS contacts
(
    contact_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)                          NOT NULL,
    email         VARCHAR(100)                          NOT NULL,
    mobile_number VARCHAR(20)                           NOT NULL,
    message       VARCHAR(500)                          NOT NULL,
    status        VARCHAR(50)       NOT NULL,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(20)                           NOT NULL,
    updated_at    TIMESTAMP   DEFAULT NULL,
    updated_by    VARCHAR(20) DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS customers
(
    customer_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)                          NOT NULL,
    email         VARCHAR(100)                          NOT NULL UNIQUE,
    mobile_number VARCHAR(20)                           NOT NULL,
    password_hash VARCHAR(500)                          NOT NULL,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(255)                           NOT NULL,
    updated_at    TIMESTAMP   DEFAULT NULL,
    updated_by    VARCHAR(255) DEFAULT NULL,
    UNIQUE KEY unique_email (email),
    UNIQUE KEY unique_mobile_number (mobile_number)
    );

-- ========================================
-- Créer la nouvelle table
-- ========================================

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- Token UUID
    token VARCHAR(36) NOT NULL UNIQUE,
    -- Relation avec customer
    customer_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    -- Sécurité
    revoked BOOLEAN DEFAULT FALSE NOT NULL,
    -- Tracking
    ip_address VARCHAR(45),        -- IPv6 max = 45 caractères
    user_agent VARCHAR(255),
    device_info VARCHAR(255),
    -- Contraintes
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Index optimisés
-- ========================================

-- Index sur token pour recherche rapide
CREATE INDEX idx_refresh_token ON refresh_tokens(token);

-- Index sur customer_id
CREATE INDEX idx_refresh_token_customer_id ON refresh_tokens(customer_id);

-- Index sur expiry_date pour nettoyage automatique
CREATE INDEX idx_refresh_token_expiry_date ON refresh_tokens(expiry_date);

-- Index composite pour requêtes actives (très performant)
CREATE INDEX idx_refresh_token_active ON refresh_tokens(customer_id, revoked, expiry_date);

-- ========================================
-- Vérification
-- ========================================

DESCRIBE refresh_tokens;
SHOW INDEX FROM refresh_tokens;

SELECT 'Table recréée avec succès !' as status;

CREATE TABLE IF NOT EXISTS address
(
    address_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id       BIGINT NOT NULL UNIQUE,
    street        VARCHAR(150) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    postal_code   VARCHAR(20)  NOT NULL,
    country       VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(255)  NOT NULL,
    updated_at    TIMESTAMP    DEFAULT NULL,
    updated_by    VARCHAR(255)  DEFAULT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS roles (
    role_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) AFTER name,
    description TEXT AFTER display_name,
    is_active BOOLEAN DEFAULT TRUE AFTER description,
    is_system BOOLEAN DEFAULT FALSE AFTER is_active;
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP   DEFAULT NULL,
    updated_by VARCHAR(100) DEFAULT NULL,
    UNIQUE KEY unique_name (name)
    );

CREATE TABLE IF NOT EXISTS customer_roles (
   customer_id BIGINT NOT NULL,
   role_id     BIGINT NOT NULL,
   PRIMARY KEY (customer_id, role_id),
   FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
   FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

-- Mettre à jour les données avec des descriptions précises
UPDATE roles SET
    display_name = 'Client',
    description = 'Client de la boutique - peut passer des commandes, consulter son historique et gérer son profil',
    is_system = TRUE
WHERE name = 'ROLE_USER';

UPDATE roles SET
    display_name = 'Administrateur',
    description = 'Administrateur système - accès complet à toutes les fonctionnalités, gestion des utilisateurs, configuration système et rapports avancés',
    is_system = TRUE
WHERE name = 'ROLE_ADMIN';

-- Remplacer ROLE_OPS_ENG par ROLE_MANAGER
UPDATE roles SET
    name = 'ROLE_MANAGER',
    display_name = 'Gestionnaire',
    description = 'Gestionnaire de boutique - peut gérer les produits, traiter les commandes, voir les rapports de vente et contacter les clients',
    is_system = TRUE
WHERE name = 'ROLE_OPS_ENG';

-- Remplacer ROLE_QA_ENG par ROLE_EMPLOYEE
UPDATE roles SET
    name = 'ROLE_EMPLOYEE',
    display_name = 'Employé',
    description = 'Employé de la boutique - peut consulter les commandes, répondre aux messages clients et mettre à jour les statuts de livraison',
    is_system = TRUE
WHERE name = 'ROLE_QA_ENG';

-- Vérification
SELECT role_id, name, display_name, description, is_active, is_system, created_at
FROM roles
ORDER BY role_id;

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_USER', CURRENT_TIMESTAMP, 'DBA');

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_ADMIN', CURRENT_TIMESTAMP, 'DBA');

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_OPS_ENG', CURRENT_TIMESTAMP, 'DBA');

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_QA_ENG', CURRENT_TIMESTAMP, 'DBA');




CREATE TABLE IF NOT EXISTS orders
(
    order_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT NOT NULL,
    total_price    DECIMAL(10, 2)                        NOT NULL,
    payment_id     VARCHAR(255)                          NOT NULL,
    payment_status VARCHAR(50)                           NOT NULL,
    order_status   VARCHAR(50)                           NOT NULL,
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR(100)                           NOT NULL,
    updated_at     TIMESTAMP   DEFAULT NULL,
    updated_by     VARCHAR(100) DEFAULT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
    );

CREATE TABLE IF NOT EXISTS order_items
(
    order_item_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    quantity        INT NOT NULL,
    price           DECIMAL(10, 2) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMP      DEFAULT NULL,
    updated_by      VARCHAR(100)    DEFAULT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    );