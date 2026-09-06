-- ============================================================
-- Player Account & Game Management System — MySQL schema
-- Run this once in MySQL Workbench / phpMyAdmin (XAMPP) / CLI:
--     mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS game_db;
USE game_db;

-- 1. Players -----------------------------------------------------
CREATE TABLE IF NOT EXISTS Players (
    PlayerID   INT AUTO_INCREMENT PRIMARY KEY,
    Name       VARCHAR(50)  NOT NULL,
    Email      VARCHAR(100) NOT NULL UNIQUE,
    Password   VARCHAR(255) NOT NULL,
    Level      INT DEFAULT 1,
    XP         INT DEFAULT 0,
    Coins      INT DEFAULT 100
);

-- 2. Characters ----------------------------------------------------
CREATE TABLE IF NOT EXISTS Characters (
    CharacterID   INT AUTO_INCREMENT PRIMARY KEY,
    PlayerID      INT NOT NULL,
    CharacterName VARCHAR(50) NOT NULL,
    Class         ENUM('Warrior','Mage','Archer') NOT NULL,
    Health        INT DEFAULT 100,
    Attack        INT DEFAULT 10,
    Defense       INT DEFAULT 10,
    Level         INT DEFAULT 1,
    FOREIGN KEY (PlayerID) REFERENCES Players(PlayerID) ON DELETE CASCADE
);

-- 3. Inventory -------------------------------------------------------
CREATE TABLE IF NOT EXISTS Inventory (
    ItemID     INT AUTO_INCREMENT PRIMARY KEY,
    PlayerID   INT NOT NULL,
    WeaponName VARCHAR(50) NOT NULL,
    Damage     INT DEFAULT 0,
    Quantity   INT DEFAULT 1,
    FOREIGN KEY (PlayerID) REFERENCES Players(PlayerID) ON DELETE CASCADE
);

-- 4. Scores / Leaderboard --------------------------------------------
CREATE TABLE IF NOT EXISTS Scores (
    ScoreID   INT AUTO_INCREMENT PRIMARY KEY,
    PlayerID  INT NOT NULL,
    HighScore INT DEFAULT 0,
    `Rank`    INT DEFAULT 0,
    FOREIGN KEY (PlayerID) REFERENCES Players(PlayerID) ON DELETE CASCADE
);

-- 5. Achievements ------------------------------------------------------
CREATE TABLE IF NOT EXISTS Achievements (
    AchievementID INT AUTO_INCREMENT PRIMARY KEY,
    PlayerID      INT NOT NULL,
    Title         VARCHAR(100) NOT NULL,
    Description   VARCHAR(255),
    Unlocked      BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (PlayerID) REFERENCES Players(PlayerID) ON DELETE CASCADE
);
