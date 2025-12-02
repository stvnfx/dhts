package dev.sf13.dhts.events;

import bt.metainfo.TorrentId;
import bt.net.Peer;

public class NewAnnounceEvent {
    private final TorrentId infoHash;
    private final Peer peer;

    public NewAnnounceEvent(TorrentId infoHash, Peer peer) {
        this.infoHash = infoHash;
        this.peer = peer;
    }

    public TorrentId getInfoHash() {
        return infoHash;
    }

    public Peer getPeer() {
        return peer;
    }
}
