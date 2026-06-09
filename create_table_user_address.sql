USE BookStoreee;
GO

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[user_address]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[user_address](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [user_id] [int] NULL,
        [receiver_name] [nvarchar](255) NULL,
        [receiver_phone] [varchar](20) NULL,
        [province_id] [int] NOT NULL,
        [province_name] [nvarchar](100) NULL,
        [district_id] [int] NOT NULL,
        [ward_code] [varchar](50) NULL,
        [ward_name] [nvarchar](100) NULL,
        [street] [nvarchar](255) NULL,
        [is_default] [bit] NULL,
        CONSTRAINT [PK_user_address] PRIMARY KEY CLUSTERED ([id] ASC)
    );

    ALTER TABLE [dbo].[user_address]  WITH CHECK ADD  CONSTRAINT [FK_user_address_Users] FOREIGN KEY([user_id])
    REFERENCES [dbo].[Users] ([id]) ON DELETE CASCADE;
END
GO
