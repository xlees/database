/**

Copyright (C) SYSTAP, LLC 2006-2007.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Oct 18, 2006
 */

package com.bigdata.journal;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.bigdata.quorum.Quorum;
import com.bigdata.rawstore.WormAddressManager;

/**
 * Interface for a root block on the journal. The root block provides metadata
 * about the journal. The journal has two root blocks. The root blocks are
 * written in an alternating order according to the Challis algorithm. Each root
 * block includes a field at the head and tail whose value is strictly
 * increasing fields. This field is often referred to as a root block
 * "timestamps", but in practice we use the commit counter. On restart, the root
 * block is choosen whose (a) strictly increasing fields agree; and (b) whose
 * value on those fields is greater. This protected against both crashes and
 * partial writes of the root block itself.
 * <p>
 * The commit counter is a store local strictly increasing non-negative long
 * integer (commit counters are distinct for each store regardless of whether
 * they are part of the same distributed database). The commit counters MUST be
 * strictly increasing (a) so that they place the commit records into a total
 * ordering; (b) so that the more current root block may be choose by comparing
 * the value of the field in each of the two root blocks; and (c) so that a
 * partial write of a root block may be detected by the presence of different
 * values for the field at the head and tail of a given root block. The commit
 * counter is also used as the field written at the head and tail of each root
 * block according to the Challis algorithm. If those fields are the same then
 * the root block is assumed to have been completely written.
 * <p>
 * Note that random data may still result in an identical value during a partial
 * write. This possibility is guarded against by storing the checksum of the
 * root block.
 * <p>
 * The first and last commit times are persisted in each root block in order to
 * support both unisolated commits and transactions, whether in a local or a
 * distributed database. These "times" are generated by the appropriate
 * {@link ITransactionManagerService} service, which is responsible both for assigning
 * transaction start times (which are in fact the transaction identifier) and
 * transaction commit times, which are stored in root blocks of the various
 * stored that participate in a given database and reported via
 * {@link #getFirstCommitTime()} and {@link #getLastCommitTime()}. While these
 * do not strictly speaking have to be "times" they do have to be assigned using
 * the same measure as the transaction identifiers, so either a coordinated time
 * server or a strictly increasing counter. Regardless, we need to know "when" a
 * transaction commits as well as "when" it starts whether we measure "when"
 * using a counter or a clock. Also note that we need to assign "commit times"
 * even when the operation is unisolated. This means that we have to coordinate
 * an unisolated commit on a store that is part of a distributed database with
 * the centralized transaction manager. This should be done as part of the group
 * commit since we are waiting at that point anyway to optimize IO by minimizing
 * syncs to disk.
 * <p>
 * Note that some file systems or disks can re-order writes of by the
 * application and write the data in a more efficient order. This can cause the
 * root blocks to be written before the application data is stable on disk. The
 * {@link Options#DOUBLE_SYNC} option exists to defeat this behavior and ensure
 * restart-safety for such systems.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IRootBlockView {

    /**
     * Assertion throws exception unless the root block is valid. Conditions
     * tested include the root block MAGIC and the root block timestamps (there
     * are two and they must agree).
     */
    public void valid() throws RootBlockException;

    /**
     * There are two root blocks and they are written in an alternating order.
     * For the sake of distinction, the first one is referred to as "rootBlock0"
     * while the 2nd one is referred to as "rootBlock1". This method indicates
     * which root block is represented by this view based on metadata supplied
     * to the constructor (the distinction is not persistent on disk).
     * 
     * @return True iff the root block view was constructed from "rootBlock0".
     */
    public boolean isRootBlock0();

    /**
     * The root block version number.
     */
    public int getVersion();
    
    /**
     * The next offset at which a data item would be written on the store.
     */
    public long getNextOffset();

    /**
     * The database wide timestamp of first commit on the store -or- 0L if there
     * have been no commits.  In a local database, this timestamp is generated by
     * a local timestamp service.  In a distributed database, this timestamp
     * is generated by a shared timestamp service. The timestamps returned by
     * this method are strictly increasing for a given store and for a given
     * database.
     * 
     * @return The timestamp of the first commit on the store or 0L iff there
     *         have been no commits.
     */
    public long getFirstCommitTime();
    
    /**
     * The database wide timestamp of the most recent commit on the store or 0L
     * iff there have been no commits. In a local database, this timestamp is
     * generated by a local timestamp service. In a distributed database, this
     * timestamp is generated by a shared timestamp service. The timestamps
     * returned by this method are strictly increasing for a given store and for
     * a given database.
     * 
     * @return The timestamp of the most recent commit on the store or 0L iff
     *         there have been no commits.
     */
    public long getLastCommitTime();
    
    /**
     * The commit counter is a positive long integer that is strictly local to
     * the store. The commit counter is used to avoid problems with timestamps
     * generated by different machines or when time goes backwards or other
     * nasty stuff. The correct root block is chosen by selecting the valid
     * root block with the larger commit counter (the value of the commit counter
     * is reused by the {@link #getChallisField() Challis field}).
     * 
     * @return The commit counter.
     */
    public long getCommitCounter();
    
    /**
     * Return the address at which the {@link ICommitRecord} for this root block
     * is stored. The {@link ICommitRecord}s are stored separately from the
     * root block so that they may be indexed by the commit timestamps. This is
     * necessary in order to be able to quickly recover the root addresses for a
     * given commit timestamp, which is a featured used to support transactional
     * isolation.
     * <p>
     * Note: When a logical journal may overflow onto more than one physical
     * journal then the address of the {@link ICommitRecord} MAY refer to a
     * historical physical journal and care MUST be exercised to resolve the
     * address against the appropriate journal file. [This paragraph is probably
     * not valid.  Verify and remove if it is not true.]
     * 
     * @return The address at which the {@link ICommitRecord} for this root
     *         block is stored.
     */
    public long getCommitRecordAddr();

    /**
     * The address of the root of the {@link CommitRecordIndex}. The
     * {@link CommitRecordIndex} contains the ordered addresses of the
     * historical {@link ICommitRecord}s on the {@link Journal}. The address
     * of the {@link CommitRecordIndex} is stored directly in the root block
     * rather than the {@link ICommitRecord} since we can not obtain this
     * address until after we have formatted and written the
     * {@link ICommitRecord}.
     */
    public long getCommitRecordIndexAddr();
    
    /**
     * The unique journal identifier
     */
    public UUID getUUID();

    /*
     * @todo Consider putting the logical service UUID into the root blocks. It
     * is already in the service Entry[] (and the file system path) for
     * scale-out.
     */
//    /**
//     * The unique identifier for the logical service to which this journal
//     * belongs. All physical services for the same logical service will have the
//     * same logical service {@link UUID}. The logical service {@link UUID} is
//     * generated when the quorum leader creates the initial journal for a
//     * service and is written into the root blocks. From the root blocks it is
//     * replicated to the {@link Quorum} followers.
//     * <p>
//     * Note: The physical service UUID is NOT stored in the root blocks since
//     * that would make the root blocks incompatible when they are replicated to
//     * other nodes in the same logical service and high availability maintains
//     * binary compatibility when replicating a journal.
//     */
//    public UUID getLogicalServiceUUID();
    
    /**
     * The #of bits in a 64-bit long integer address that are dedicated to the
     * byte offset into the store.
     * 
     * @see WormAddressManager
     */
    public int getOffsetBits();
    
    /**
     * The timestamp assigned as the creation time for the journal.
     */
    public long getCreateTime();

    /**
     * The timestamp assigned as the time at which writes were disallowed for
     * the journal.
     */
    public long getCloseTime();

    /**
     * A byte value which specifies whether the backing store is a journal
     * (log-structured store or WORM) or a read-write store. Only two values are
     * defined at present. ZERO (0) is a WORM; ONE (1) is a read/write store.
     */
    public StoreTypeEnum getStoreType();

    /**
     * For the {@link StoreTypeEnum#RW} store, where we will read the metadata
     * bits from. When we start the store up we need to retrieve the metabits
     * from this address. This is a byte offset into the file and is stored as a
     * long integer. Normal addresses are calculated with reference to the
     * allocation blocks. The value for a WORM store is ZERO (0).
     */
    public long getMetaBitsAddr();

    /**
     * For the {@link StoreTypeEnum#RW} store, the start of the area of the file
     * where the allocation blocks are allocated. This is also a byte offset
     * into the file and is stored as a 64-bit integer. It is called
     * metaStartAddr because that is the offset that is used with the
     * metaBitsAddr to determine how to find the allocation blocks. The value
     * for a WORM store is ZERO (0).
     */
    public long getMetaStartAddr();

    /**
     * The {@link Quorum} token associated with this commit point or
     * {@link Quorum#NO_QUORUM} if there was no quorum.
     * <p>
     * Note: If commit points are part of the resynchronization protocol, they
     * MUST NOT use the current quorum token unless the service is synchronized
     * with the quorum at that commit point.
     */
    public long getQuorumToken();

    /**
     * A read-only buffer whose contents are the root block. The position,
     * limit, and mark will be independent for each {@link ByteBuffer} that is
     * returned by this method.
     */
    public ByteBuffer asReadOnlyBuffer();

}
