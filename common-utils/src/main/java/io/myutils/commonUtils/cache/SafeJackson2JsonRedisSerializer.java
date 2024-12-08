package io.myutils.commonUtils.cache;

import jakarta.annotation.Nullable;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

public class SafeJackson2JsonRedisSerializer extends GenericJackson2JsonRedisSerializer {

    // add logger

    public SafeJackson2JsonRedisSerializer() { super(); }

    @Override
    @Nullable
    public <T> T deserialize(@Nullable byte[] source, Class<T> type){
        try {
            return super.deserialize(source, type);
        } catch(Exception e)
        {
            // add log
        }
        return null;
    }
}
