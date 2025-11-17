USE eazystore;

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Mbappé', 'Sticker Kylian Mbappé - Phénoménal joueur de football français! ⚡', 8.00, 95,
 (SELECT category_id FROM categories WHERE code = 'SPORTS'), 'STK-MBAPPE-001', 100,
 '/uploads/products/main/Mbappe.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Ronaldo', 'Sticker Cristiano Ronaldo - Légende du football! CR7 🏆', 8.00, 100,
 (SELECT category_id FROM categories WHERE code = 'SPORTS'), 'STK-RONALDO-002', 120,
 '/uploads/products/main/ronaldo.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Messi', 'Sticker Lionel Messi - Le magicien argentin! 🐐', 10.00, 99,
 (SELECT category_id FROM categories WHERE code = 'SPORTS'), 'STK-MESSI-003', 150,
 '/uploads/products/main/Messi.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Virat Kohli', 'Sticker Virat Kohli - Le roi du cricket indien! 👑', 9.00, 99,
 (SELECT category_id FROM categories WHERE code = 'SPORTS'), 'STK-VIRAT-004', 100,
 '/uploads/products/main/Virat.png', 1, CURRENT_TIMESTAMP, 'admin');

-- ==================== ANIME (3 produits) ====================

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Naruto', 'Sticker Naruto Uzumaki - Le ninja le plus déterminé! 🍥', 6.00, 88,
 (SELECT category_id FROM categories WHERE code = 'ANIME'), 'STK-NARUTO-005', 150,
 '/uploads/products/main/Naruto.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Goku', 'Sticker Son Goku - Le Saiyan légendaire! ⚡', 6.00, 92,
 (SELECT category_id FROM categories WHERE code = 'ANIME'), 'STK-GOKU-006', 140,
 '/uploads/products/main/Goku.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Shin-Chan', 'Sticker Shin-Chan - L''enfant le plus espiègle! 😄', 5.00, 75,
 (SELECT category_id FROM categories WHERE code = 'ANIME'), 'STK-SHINCHAN-007', 120,
 '/uploads/products/main/Shinchan.png', 1, CURRENT_TIMESTAMP, 'admin');

-- ==================== GAMING (2 produits) ====================


INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Game Over', 'Sticker Game Over - Pour les vrais gamers! 🎮', 5.00, 70,
 (SELECT category_id FROM categories WHERE code = 'GAMING'), 'STK-GAMEOVER-008', 100,
 '/uploads/products/main/GameOver.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Squid Game', 'Sticker Squid Game - Jouons ensemble! 🔴⚪', 5.00, 92,
 (SELECT category_id FROM categories WHERE code = 'GAMING'), 'STK-SQUIDGAME-009', 180,
 '/uploads/products/main/SquidGame.png', 1, CURRENT_TIMESTAMP, 'admin');

-- ==================== CODING (6 produits) ====================

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Développeur', 'Sticker Développeur - Assistant de code indispensable! 💻', 5.00, 85,
 (SELECT category_id FROM categories WHERE code = 'CODING'), 'STK-DEV-010', 150,
 '/uploads/products/main/developer.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Break', 'Sticker Break - Prenons une pause et recommençons! ☕', 4.50, 60,
 (SELECT category_id FROM categories WHERE code = 'CODING'), 'STK-BREAK-011', 100,
 '/uploads/products/main/break.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Ce n''est pas un bug', 'Sticker - C''est une fonctionnalité surprise! 🐛', 6.00, 98,
 (SELECT category_id FROM categories WHERE code = 'CODING'), 'STK-NOTABUG-012', 200,
 '/uploads/products/main/itsnotabug.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Eat Sleep Code', 'Sticker Devster - Le cycle de vie du développeur! 🔄', 5.00, 72,
 (SELECT category_id FROM categories WHERE code = 'CODING'), 'STK-EATSLEEPC-013', 120,
 '/uploads/products/main/EatSleepCode.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Code Smasher', 'Sticker CodeSmasher - Développeur intrépide qui casse du code! 💪', 7.50, 88,
 (SELECT category_id FROM categories WHERE code = 'CODING'), 'STK-CODESMASHER-014', 100,
 '/uploads/products/main/BreakingCode.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('You Are My CSS', 'Sticker CodeMate - Sans toi, je suis incomplet! 💝', 2.00, 79,
 (SELECT category_id FROM categories WHERE code = 'CODING'), 'STK-CSS-015', 180,
 '/uploads/products/main/youaremycss.png', 1, CURRENT_TIMESTAMP, 'admin');

-- ==================== HUMOR (6 produits) ====================

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Ma conduite me fait peur', 'Sticker humour - La vérité sur ma conduite! 🚗', 5.00, 65,
 (SELECT category_id FROM categories WHERE code = 'HUMOR'), 'STK-DRIVING-016', 100,
 '/uploads/products/main/MyDrivingScaresMeToo.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Boo', 'Sticker Boo - Huées et désapprobation! 👎', 6.00, 60,
 (SELECT category_id FROM categories WHERE code = 'HUMOR'), 'STK-BOO-017', 100,
 '/uploads/products/main/Boo.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Ew Feelings', 'Sticker - Les sentiments? Non merci! 😒', 6.00, 68,
 (SELECT category_id FROM categories WHERE code = 'HUMOR'), 'STK-EWFEEL-018', 120,
 '/uploads/products/main/EwFeelings.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Be Wild', 'Sticker Be Wild - Déchaîne-toi! 🦁', 6.00, 70,
 (SELECT category_id FROM categories WHERE code = 'HUMOR'), 'STK-BEWILD-019', 110,
 '/uploads/products/main/BeWild.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Your Opinion Means Nothing', 'Sticker Sauvagerie - Ton avis ne compte pas! 😎', 6.00, 75,
 (SELECT category_id FROM categories WHERE code = 'HUMOR'), 'STK-OPINION-020', 130,
 '/uploads/products/main/YourOpinonMeansNothing.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Socially Awkward', 'Sticker - Gênant et étrange, c''est moi! 😬', 6.00, 72,
 (SELECT category_id FROM categories WHERE code = 'HUMOR'), 'STK-AWKWARD-021', 100,
 '/uploads/products/main/SociallyAwkward.png', 1, CURRENT_TIMESTAMP, 'admin');

-- ==================== ANIMALS (2 produits) ====================

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Lazy Cat', 'Sticker Chat Paresseux - Pas aujourd''hui! 😴', 6.00, 78,
 (SELECT category_id FROM categories WHERE code = 'ANIMALS'), 'STK-LAZYCAT-022', 140,
 '/uploads/products/main/LazyCat.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Aesthetic Summer Cat', 'Sticker Chat d''été - Moustaches de canicule! ☀️', 6.00, 82,
 (SELECT category_id FROM categories WHERE code = 'ANIMALS'), 'STK-SUMMERCAT-023', 120,
 '/uploads/products/main/AestheticSummerCat.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Butterfly', 'Sticker Papillon bleu - Gracewing élégant! 🦋', 6.00, 80,
 (SELECT category_id FROM categories WHERE code = 'ANIMALS'), 'STK-BUTTERFLY-024', 120,
 '/uploads/products/main/Butterfly.png', 1, CURRENT_TIMESTAMP, 'admin');

-- ==================== LIFESTYLE (4 produits) ====================

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Cool Astronaut', 'Sticker AstroChill - Cool pour la gravité! 🚀', 3.00, 65,
 (SELECT category_id FROM categories WHERE code = 'LIFESTYLE'), 'STK-ASTRO-025', 100,
 '/uploads/products/main/CoolAstraunaut.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Over Thinker', 'Sticker Cerveau occupé - Penseur excessif! 🧠', 4.00, 70,
 (SELECT category_id FROM categories WHERE code = 'LIFESTYLE'), 'STK-OVERTHINKER-026', 110,
 '/uploads/products/main/OverThinker.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('I Am Okay', 'Sticker Je vais bien - Persévérant malgré tout! 💪', 6.00, 68,
 (SELECT category_id FROM categories WHERE code = 'LIFESTYLE'), 'STK-OKAY-027', 130,
 '/uploads/products/main/IamOkay.png', 1, CURRENT_TIMESTAMP, 'admin');

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('I Won''t Hesitate', 'Sticker Sans hésitation - Toujours prêt! ⚡', 6.00, 74,
 (SELECT category_id FROM categories WHERE code = 'LIFESTYLE'), 'STK-NOHESITATE-028', 100,
 '/uploads/products/main/IWon_tHesitateSticker.png', 1, CURRENT_TIMESTAMP, 'admin');

-- ==================== TV SHOWS (1 produit) ====================

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('House of the Dragon', 'Sticker Symbole Targaryen - La force de la dynastie! 🐉', 9.00, 98,
 (SELECT category_id FROM categories WHERE code = 'TV_SHOWS'), 'STK-HOTD-029', 150,
 '/uploads/products/main/HouseOfTheDragonSymbol.png', 1, CURRENT_TIMESTAMP, 'admin');

-- ==================== OTHER (1 produit) ====================

INSERT INTO products (name, description, price, popularity, category_id, sku, stock_quantity, image_url, is_active, created_at, created_by) VALUES
('Evil Eye', 'Sticker Wardgaze - Pouvoir protecteur du mauvais œil! 🧿', 6.00, 76,
 (SELECT category_id FROM categories WHERE code = 'OTHER'), 'STK-EVILEYE-030', 100,
 '/uploads/products/main/EvilEye.png', 1, CURRENT_TIMESTAMP, 'admin');



-- Insérer les catégories
INSERT INTO categories (code, name, description, icon, display_order, is_active, created_at, created_by) VALUES
('SPORTS', 'Sports', 'Stickers de sportifs et athlètes célèbres', '⚽', 1, 1, CURRENT_TIMESTAMP, 'admin'),
('ANIME', 'Anime & Manga', 'Personnages d''animation japonaise', '🎌', 2, 1, CURRENT_TIMESTAMP, 'admin'),
('GAMING', 'Jeux Vidéo', 'Culture gaming et jeux vidéo', '🎮', 3, 1, CURRENT_TIMESTAMP, 'admin'),
('CODING', 'Code & Tech', 'Humour et culture de développeurs', '💻', 4, 1, CURRENT_TIMESTAMP, 'admin'),
('HUMOR', 'Humour', 'Stickers drôles et humoristiques', '😄', 5, 1, CURRENT_TIMESTAMP, 'admin'),
('ANIMALS', 'Animaux', 'Animaux mignons et drôles', '🐱', 6, 1, CURRENT_TIMESTAMP, 'admin'),
('LIFESTYLE', 'Style de Vie', 'Vie quotidienne et émotions', '🌟', 7, 1, CURRENT_TIMESTAMP, 'admin'),
('TV_SHOWS', 'Séries TV', 'Personnages de séries populaires', '📺', 8, 1, CURRENT_TIMESTAMP, 'admin'),
('OTHER', 'Autre', 'Stickers divers et variés', '📦', 99, 1, CURRENT_TIMESTAMP, 'admin');



-- =====================================================
-- UPDATE DES 30 PRODUITS AVEC LEURS CATÉGORIES
-- =====================================================
-- Base: eazystore
-- Date: 2025-10-24
-- 30 stickers à catégoriser
-- =====================================================

USE eazystore;

-- 🔓 Désactiver Safe Mode pour permettre les UPDATE
SET SQL_SAFE_UPDATES = 0;

-- =====================================================
-- 💻 CODING - 6 produits
-- =====================================================

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'CODING')
WHERE name = 'Développeur';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'CODING')
WHERE name = 'Casser';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'CODING')
WHERE name = 'Ce n''est pas un bug';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'CODING')
WHERE name = 'Devster';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'CODING')
WHERE name = 'CodeSmasher';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'CODING')
WHERE name = 'CodeMate';

-- =====================================================
-- ⚽ SPORTS - 4 produits
-- =====================================================

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'SPORTS')
WHERE name = 'Mbappé';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'SPORTS')
WHERE name = 'Ronaldo';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'SPORTS')
WHERE name = 'Messi';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'SPORTS')
WHERE name = 'Virat Kohli';

-- =====================================================
-- 🎌 ANIME - 3 produits
-- =====================================================

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'ANIME')
WHERE name = 'Shin-Chan';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'ANIME')
WHERE name = 'Naruto';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'ANIME')
WHERE name = 'Goku';

-- =====================================================
-- 🎮 GAMING - 2 produits
-- =====================================================

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'GAMING')
WHERE name = 'Jeu du calmar';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'GAMING')
WHERE name = 'Game over';

-- =====================================================
-- 📺 TV_SHOWS - 1 produit
-- =====================================================

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'TV_SHOWS')
WHERE name = 'Symbole du dragon à trois têtes';

-- =====================================================
-- 🐱 ANIMALS - 3 produits
-- =====================================================

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'ANIMALS')
WHERE name = 'Chat paresseux';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'ANIMALS')
WHERE name = 'Chat d''été';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'ANIMALS')
WHERE name = 'Papillon bleu';

-- =====================================================
-- 🌟 LIFESTYLE - 7 produits
-- =====================================================

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'LIFESTYLE')
WHERE name = 'AstroChill';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'LIFESTYLE')
WHERE name = 'Ma conduite me fait peur aussi';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'LIFESTYLE')
WHERE name = 'Cerveau occupé';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'LIFESTYLE')
WHERE name = 'Je vais bien';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'LIFESTYLE')
WHERE name = 'Sentiment EW';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'LIFESTYLE')
WHERE name = 'Soyez sauvage';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'LIFESTYLE')
WHERE name = 'Gênant';

-- =====================================================
-- 😄 HUMOR - 4 produits
-- =====================================================

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'HUMOR')
WHERE name = 'Huer';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'HUMOR')
WHERE name = 'Sauvagerie';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'HUMOR')
WHERE name = 'Aucune hésitation';

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE code = 'HUMOR')
WHERE name = 'Wardgaze';

-- =====================================================
-- VÉRIFICATION : Produits sans catégorie
-- =====================================================

SELECT
    COUNT(*) as produits_sans_categorie,
    CASE
        WHEN COUNT(*) = 0 THEN '✅ Tous les produits ont une catégorie'
        ELSE '⚠️ Certains produits n''ont pas de catégorie'
    END as statut
FROM products
WHERE category_id IS NULL;

-- Afficher les produits sans catégorie (s'il y en a)
SELECT
    product_id,
    name,
    'SANS CATÉGORIE' as probleme
FROM products
WHERE category_id IS NULL;

-- =====================================================
-- RÉSUMÉ PAR CATÉGORIE
-- =====================================================

SELECT
    c.icon as '🎨',
    c.name as 'Catégorie',
    COUNT(p.product_id) as 'Nb Produits',
    ROUND(COUNT(p.product_id) * 100.0 / (SELECT COUNT(*) FROM products), 1) as '%',
    GROUP_CONCAT(p.name ORDER BY p.name SEPARATOR ', ') as 'Produits'
FROM categories c
LEFT JOIN products p ON c.category_id = p.category_id
GROUP BY c.category_id, c.icon, c.name
ORDER BY c.display_order;

-- =====================================================
-- LISTE COMPLÈTE DES PRODUITS AVEC CATÉGORIES
-- =====================================================

SELECT
    p.product_id as 'ID',
    c.icon as '🎨',
    c.name as 'Catégorie',
    p.name as 'Produit',
    CONCAT(p.price, ' €') as 'Prix',
    p.popularity as 'Pop'
FROM products p
LEFT JOIN categories c ON p.category_id = c.category_id
ORDER BY c.display_order, p.name;

-- 🔒 Réactiver Safe Mode
SET SQL_SAFE_UPDATES = 1;

-- =====================================================
-- STATISTIQUES FINALES
-- =====================================================

SELECT
    '🎉 MIGRATION TERMINÉE !' as '=== STATUT ===',
    (SELECT COUNT(*) FROM products) as 'Total Produits',
    (SELECT COUNT(*) FROM products WHERE category_id IS NOT NULL) as 'Produits Catégorisés',
    (SELECT COUNT(*) FROM products WHERE category_id IS NULL) as 'Sans Catégorie',
    (SELECT COUNT(DISTINCT category_id) FROM products WHERE category_id IS NOT NULL) as 'Catégories Utilisées'
FROM DUAL;


/*
RÉCAPITULATIF DES 30 PRODUITS :

💻 CODING (6) :
   1. Développeur
   2. Casser
   3. Ce n'est pas un bug
   4. Devster
   5. CodeSmasher
   6. CodeMate

⚽ SPORTS (4) :
   7. Mbappé
   8. Ronaldo
   9. Messi
   10. Virat Kohli

🎌 ANIME (3) :
   11. Shin-Chan
   12. Naruto
   13. Goku

🎮 GAMING (2) :
   14. Jeu du calmar
   15. Game over

📺 TV_SHOWS (1) :
   16. Symbole du dragon à trois têtes

🐱 ANIMALS (3) :
   17. Chat paresseux
   18. Chat d'été
   19. Papillon bleu

🌟 LIFESTYLE (7) :
   20. AstroChill
   21. Ma conduite me fait peur aussi
   22. Cerveau occupé
   23. Je vais bien
   24. Sentiment EW
   25. Soyez sauvage
   26. Gênant

😄 HUMOR (4) :
   27. Huer
   28. Sauvagerie
   29. Aucune hésitation
   30. Wardgaze

TOTAL : 30 produits
*/