USE BookStoreee;
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[banners]') AND name = 'start_date')
BEGIN
    ALTER TABLE [dbo].[banners] ADD [start_date] DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[banners]') AND name = 'end_date')
BEGIN
    ALTER TABLE [dbo].[banners] ADD [end_date] DATETIME2 NULL;
END
GO
