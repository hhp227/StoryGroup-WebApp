package kr.hhp227.groupsns_webapp.comment

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper

@Mapper
interface UserReplyMapper {
    @Insert("INSERT INTO user_replys(reply_id, user_id, post_id) VALUES(#{replyId}, #{userId}, #{postId})")
    fun insert(record: NewUserReplyRecord): Int
}
