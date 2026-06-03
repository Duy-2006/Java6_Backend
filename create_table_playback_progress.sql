-- SQL Server Script to Create AUDIO_PLAYBACK_PROGRESS table
-- Database: BookStoreee

-- 1. Check if the table already exists, if so drop it or skip
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[AUDIO_PLAYBACK_PROGRESS]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[AUDIO_PLAYBACK_PROGRESS] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [user_id] INT NOT NULL,
        [book_id] INT NOT NULL,
        [chapter_id] BIGINT NOT NULL,
        [segment_index] INT DEFAULT 0,
        [current_time_seconds] FLOAT DEFAULT 0.0,
        [playback_rate] FLOAT DEFAULT 1.0,
        [updated_at] DATETIME DEFAULT GETDATE(),
        
        -- Foreign Key relationships
        CONSTRAINT [FK_AUDIO_PROGRESS_USER] FOREIGN KEY ([user_id]) REFERENCES [Users]([id]) ON DELETE CASCADE,
        CONSTRAINT [FK_AUDIO_PROGRESS_BOOK] FOREIGN KEY ([book_id]) REFERENCES [Books]([id]) ON DELETE CASCADE,
        
        -- Unique constraint to prevent duplicate entries for user/book combination
        CONSTRAINT [UC_AUDIO_PROGRESS_USER_BOOK] UNIQUE ([user_id], [book_id])
    );

    PRINT 'Table AUDIO_PLAYBACK_PROGRESS created successfully!';
END
ELSE
BEGIN
    PRINT 'Table AUDIO_PLAYBACK_PROGRESS already exists.';
END
GO
