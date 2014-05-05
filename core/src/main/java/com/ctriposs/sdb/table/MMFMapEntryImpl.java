package com.ctriposs.sdb.table;

import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * Memory mapped file entry implementation
 * 
 * @author bulldog
 *
 */
public class MMFMapEntryImpl implements IMapEntry {
	
	private int index;
	
	private MappedByteBuffer dataMappedByteBuffer;
	private MappedByteBuffer indexMappedByteBuffer;
	
	public MMFMapEntryImpl(int index, MappedByteBuffer indexMappedByteBuffer, MappedByteBuffer dataMappedByteBuffer) {
		this.index = index;
		this.dataMappedByteBuffer = dataMappedByteBuffer;
		this.indexMappedByteBuffer = indexMappedByteBuffer;
	}
	
	private int getKeyLength() {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_KEY_LENGTH_OFFSET;
		return this.indexMappedByteBuffer.getInt(offsetInIndexFile);
	}
	
	private int getValueLength() {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_VALUE_LENGTH_OFFSET;
		return this.indexMappedByteBuffer.getInt(offsetInIndexFile);
	}
	
	long getItemOffsetInDataFile() {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index;
		return this.indexMappedByteBuffer.getLong(offsetInIndexFile);
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public byte[] getKey() throws IOException {
		int itemOffsetInDataFile = (int)this.getItemOffsetInDataFile();
		int keyLength = this.getKeyLength();
		byte[] result = new byte[keyLength];
		for(int i = 0; i <  keyLength; i++) {
			result[i] = this.dataMappedByteBuffer.get(i + itemOffsetInDataFile);
		}
		return result;
	}

	@Override
	public byte[] getValue() throws IOException {
		int itemOffsetInDataFile = (int)this.getItemOffsetInDataFile();
		int keyLength = this.getKeyLength();
		itemOffsetInDataFile += keyLength;
		int valueLength = this.getValueLength();
		byte[] result = new byte[valueLength];
		for(int i = 0; i <  valueLength; i++) {
			result[i] = this.dataMappedByteBuffer.get(i + itemOffsetInDataFile);
		}
		
		return result;
	}
	

	@Override
	public int getKeyHash() throws IOException {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_KEY_HASH_CODE_OFFSET;
		int hashCode = this.indexMappedByteBuffer.getInt(offsetInIndexFile);
		return hashCode;
	}

	@Override
	public long getTimeToLive() throws IOException {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_TIME_TO_LIVE_OFFSET;
		return this.indexMappedByteBuffer.getLong(offsetInIndexFile);
	}

	@Override
	public long getLastAccessedTime() throws IOException {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_LAST_ACCESSED_TIME_OFFSET;
		return this.indexMappedByteBuffer.getLong(offsetInIndexFile);
	}

	@Override
	public void setLastAccessedTime(long lastAccessedTime) throws IOException {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_LAST_ACCESSED_TIME_OFFSET;
		this.indexMappedByteBuffer.putLong(offsetInIndexFile, lastAccessedTime);
	}

	@Override
	public boolean isDeleted() throws IOException {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_STATUS;
		byte status = this.indexMappedByteBuffer.get(offsetInIndexFile);
		return (status & ( 1 << 1)) != 0;
	}
	
	@Override
	public boolean isCompressed() throws IOException {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_STATUS;
		byte status = this.indexMappedByteBuffer.get(offsetInIndexFile);
		return (status & ( 1 << 2)) != 0;
	}

	@Override
	public void markDeleted() throws IOException {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_STATUS;
		byte status = this.indexMappedByteBuffer.get(offsetInIndexFile);
		status = (byte) (status | 1 << 1);
		this.indexMappedByteBuffer.put((int)offsetInIndexFile, status);
	}

	@Override
	public boolean isInUse() throws IOException {
		int offsetInIndexFile = AbstractMapTable.INDEX_ITEM_LENGTH * index + IMapEntry.INDEX_ITEM_STATUS;
		byte status = this.indexMappedByteBuffer.get(offsetInIndexFile);
		return (status & 1) != 0;
	}

	@Override
	public boolean isExpired() throws IOException {
		long ttl = this.getTimeToLive();
		if (ttl > 0) {
			if (System.currentTimeMillis() - this.getLastAccessedTime() > ttl) return true;
		}
		return false;
	}
}
