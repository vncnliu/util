package top.vncnliu.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * User: vncnliu
 * Date: 2018/8/14
 * Description: 内存缓存
 */
public class MemCache<K, V> {

    /**
     * 过期时间毫秒
     */
    private int expireTime;
    /**
     * 过期时执行操作
     */
    private Consumer<K> consumer;
    /**
     * 正向访问缓存
     */
    private Map<K, Data> hashMapPositive = new ConcurrentHashMap<>();
    /**
     * 反向访问缓存
     */
    private Map<V, K> hashMapReverse = new ConcurrentHashMap<>();

    /**
     * @param cleanUpDelayTime 定时清理间隔
     */
    public MemCache(int expireTime, int cleanUpDelayTime, Consumer<K> consumer) {
        this.expireTime = expireTime;
        this.consumer = consumer;
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<K, Data> kDataEntry : hashMapPositive.entrySet()) {
                if(kDataEntry.getValue().expireTime<System.currentTimeMillis()){
                    hashMapPositive.remove(kDataEntry.getKey());
                    hashMapReverse.remove(kDataEntry.getValue().value);
                    if(consumer!=null){
                        consumer.accept(kDataEntry.getKey());
                    }
                }
            }
        },cleanUpDelayTime,cleanUpDelayTime,TimeUnit.MILLISECONDS);
    }

    public void put(K key, V value){
        hashMapPositive.put(key, new Data(value,System.currentTimeMillis()+expireTime));
        hashMapReverse.put(value,key);
    }

    public V get(K key){
        Data data = hashMapPositive.get(key);
        if(data!=null){
            if(System.currentTimeMillis()>data.expireTime){
                return null;
            } else {
                return data.value;
            }
        } else {
            return null;
        }
    }

    public V removeByKey(K key){
        Data data = hashMapPositive.remove(key);
        if(data!=null){
            if(consumer!=null){
                consumer.accept(key);
            }
            hashMapReverse.remove(data.value);
            return data.value;
        }
        return null;
    }

    public V removeByValue(V value){
        K key = hashMapReverse.remove(value);
        if(key!=null){
            Data data = hashMapPositive.remove(key);
            if(consumer!=null){
                consumer.accept(key);
            }
            return data.value;
        }
        return null;
    }

    private class Data {
        Data(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        private V value;
        private long expireTime;
    }

}
