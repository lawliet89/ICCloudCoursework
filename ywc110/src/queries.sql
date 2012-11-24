CREATE TABLE Cloud_Playlist(
    PlaylistId SERIAL NOT NULL PRIMARY KEY,
    PlaylistName text NOT NULL,
    UserId text NOT NULL
);

CREATE TABLE Cloud_Item(
    ItemId SERIAL NOT NULL,
    UserId text NOT NULL,
    ItemTitle text NOT NULL,
    ItemArtist text NOT NULL,
    ItemAlbum text NOT NULL,
    ItemYear int NOT NULL,
    Itemkey text NOT NULL
    CONSTRAINT cloud_item_pkey PRIMARY KEY (ItemId)
);

CREATE TABLE Cloud_PlaylistItem(
    ItemId int NOT NULL,
    PlaylistId int NOT NULL,
    CONSTRAINT cloud_playlistitem_pkey PRIMARY KEY (ItemId, PlaylistId),
    FOREIGN KEY (ItemId) REFERENCES Cloud_Item(ItemId) ON DELETE CASCADE,
    FOREIGN KEY (PlaylistId) REFERENCES Cloud_Playlist(PlaylistId) ON DELETE CASCADE
);