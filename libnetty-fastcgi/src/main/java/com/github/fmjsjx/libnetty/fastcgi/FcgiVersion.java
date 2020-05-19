package com.github.fmjsjx.libnetty.fastcgi;

/**
 * FastCGI protocol version
 *
 * @since 1.0
 *
 * @author MJ Fang
 */
public class FcgiVersion {

    /**
     * Version {@code 1}.
     */
    public static final FcgiVersion VERSION_1 = new FcgiVersion(1);

    /**
     * Returns the {@link FcgiVersion} instance representing the specified version.
     * 
     * @param version the number of the version
     * @return the {@code FcgiVersion}
     */
    public static final FcgiVersion valueOf(int version) {
        if (version == 1) {
            return VERSION_1;
        }
        return new FcgiVersion(version);
    }

    private final int version;

    FcgiVersion(int version) {
        this.version = version;
    }

    /**
     * Returns the number of this version.
     * 
     * @return the number of this version
     */
    public int version() {
        return version;
    }

    @Override
    public int hashCode() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FcgiVersion) {
            return ((FcgiVersion) obj).version == version;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FcgiVersion(" + version + ")";
    }

}
