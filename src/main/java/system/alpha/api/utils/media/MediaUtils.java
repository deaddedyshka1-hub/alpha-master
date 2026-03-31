package system.alpha.api.utils.media;

import by.bonenaut7.mediatransport4j.api.MediaSession;
import by.bonenaut7.mediatransport4j.api.MediaTransport;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaUtils {
    private static boolean initialized = false;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static volatile MediaInfo mediaInfo = null;

    private static final Map<String, AbstractTexture> textureCache = new ConcurrentHashMap<>();
    private static String previousHash = "";

    public static class MediaInfo {
        public final String title;
        public final String artist;
        public final String textureHash;

        public MediaInfo(String title, String artist, String textureHash) {
            this.title = title;
            this.artist = artist;
            this.textureHash = textureHash;
        }

        public AbstractTexture getTexture() {
            return textureCache.get(textureHash);
        }
    }

    public static MediaInfo getCurrentMedia() {
        if (!initialized) {
            MediaTransport.init();
            initialized = true;

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    List<MediaSession> sessions = MediaTransport.getMediaSessions();
                    if (sessions != null && !sessions.isEmpty()) {
                        MediaSession session = sessions.get(0);
                        String hash = "";

                        if (session.hasThumbnail()) {
                            ByteBuffer buffer = session.getThumbnail();
                            hash = hashBuffer(buffer);

                            if (!hash.equals(previousHash)) {
                                AbstractTexture old = textureCache.remove(previousHash);
                                if (old != null) old.close();

                                AbstractTexture texture = convertTexture(buffer);
                                if (texture != null) {
                                    textureCache.put(hash, texture);
                                }
                                previousHash = hash;
                            }
                        }

                        mediaInfo = new MediaInfo(session.getTitle(), session.getArtist(), hash);
                    } else {
                        clearCache();
                        mediaInfo = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
        }
        return mediaInfo;
    }

    private static AbstractTexture convertTexture(ByteBuffer buffer) {
        try {
            byte[] bytes = toByteArray(buffer);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) {
                return convert(img);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static AbstractTexture convert(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        NativeImage img = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setColorArgb(x, y, image.getRGB(x, y));
            }
        }
        return new NativeImageBackedTexture(img);
    }

    private static String hashBuffer(ByteBuffer buffer) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(toByteArray(buffer));
            return toHex(md.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static byte[] toByteArray(ByteBuffer buffer) {
        ByteBuffer duplicate = buffer.asReadOnlyBuffer();
        duplicate.clear();
        byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return bytes;
    }

    private static void clearCache() {
        textureCache.values().forEach(AbstractTexture::close);
        textureCache.clear();
        previousHash = "";
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit((b & 0xF), 16));
        }
        return sb.toString();
    }
}
