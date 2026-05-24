package com.oj.contest.service.impl;

import com.oj.api.UserClient;
import com.oj.api.dto.UserFeignDTO;
import com.oj.common.context.BaseContext;
import com.oj.common.result.Result;
import com.oj.contest.dto.CommentDTO;
import com.oj.contest.entity.SolutionComment;
import com.oj.contest.mapper.CommentMapper;
import com.oj.contest.service.SolutionCommentService;
import com.oj.contest.vo.ScrollResult;
import com.oj.contest.vo.SolutionVO;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SolutionCommentServiceImpl implements SolutionCommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private Parser parser;

    @Autowired
    private HtmlRenderer renderer;

    @Autowired
    private UserClient userClient;

    private static final String SOLUTION_KEY = "solution:problem:";
    private static final String COMMENT_KEY = "solution:comments:";
    private static final int PAGE_SIZE = 5;

    @Override
    public void save(CommentDTO commentDTO) {
        Long currentId = BaseContext.getCurrentId();
        SolutionComment solutionComment = new SolutionComment();
        BeanUtils.copyProperties(commentDTO, solutionComment);
        solutionComment.setUserId(currentId);
        solutionComment.setCreateTime(LocalDateTime.now());
        solutionComment.setUpdateTime(LocalDateTime.now());
        solutionComment.setType(1);
        commentMapper.insert(solutionComment);

        String key = SOLUTION_KEY + solutionComment.getProblemId();
        stringRedisTemplate.opsForZSet().add(key, solutionComment.getId().toString(), System.currentTimeMillis());
    }

    @Override
    public Result<ScrollResult> querySolutions(Long problemId, Long lastId, Integer offset) {
        String key = SOLUTION_KEY + problemId;
        return Result.success(scrollQuery(key, lastId, offset));
    }

    @Override
    public Result saveComment(CommentDTO commentDTO) {
        Long currentId = BaseContext.getCurrentId();
        SolutionComment solutionComment = new SolutionComment();
        BeanUtils.copyProperties(commentDTO, solutionComment);
        solutionComment.setUserId(currentId);
        solutionComment.setCreateTime(LocalDateTime.now());
        solutionComment.setUpdateTime(LocalDateTime.now());
        solutionComment.setType(2);
        solutionComment.setLikeCount(0);
        solutionComment.setCommentCount(0);
        solutionComment.setViewCount(0);
        solutionComment.setStatus(1);
        commentMapper.insert(solutionComment);

        String key = COMMENT_KEY + solutionComment.getProblemId();
        stringRedisTemplate.opsForZSet().add(key, solutionComment.getId().toString(), System.currentTimeMillis());
        return Result.success(solutionComment.getId());
    }

    @Override
    public Result<ScrollResult> queryComments(Long problemId, Long lastId, Integer offset) {
        String key = COMMENT_KEY + problemId;
        return Result.success(scrollQuery(key, lastId, offset));
    }

    @Override
    public Result getSolutionDetail(Long id) {
        SolutionComment solutionComment = commentMapper.selectById(id);
        if (solutionComment == null) {
            return Result.error("题解不存在");
        }
        SolutionVO solutionVO = new SolutionVO();
        BeanUtils.copyProperties(solutionComment, solutionVO);

        // Markdown渲染
        if (solutionVO.getContent() != null) {
            String contentHtml = renderer.render(parser.parse(solutionVO.getContent()));
            solutionVO.setContentHtml(contentHtml);
        }

        // 增加浏览量
        solutionComment.setViewCount(solutionComment.getViewCount() + 1);
        commentMapper.updateById(solutionComment);

        // 通过Feign获取用户名
        if (solutionVO.getUserId() != null) {
            Result<UserFeignDTO> userResult = userClient.getUserById(solutionVO.getUserId());
            if (userResult != null && userResult.getData() != null) {
                solutionVO.setUsername(userResult.getData().getUsername());
            }
        }

        return Result.success(solutionVO);
    }

    private ScrollResult scrollQuery(String key, Long max, Integer offset) {
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, PAGE_SIZE);

        ScrollResult result = new ScrollResult();
        if (typedTuples == null || typedTuples.isEmpty()) {
            result.setList(Collections.emptyList());
            result.setMinTime(0L);
            result.setOffset(0);
            return result;
        }

        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int newOffset = 1;

        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            long score = tuple.getScore().longValue();
            if (score == minTime) {
                newOffset++;
            } else {
                minTime = score;
                newOffset = 1;
            }
        }
        newOffset = (minTime == max) ? newOffset + offset : newOffset;

        // 按ID顺序查出完整数据（不再JOIN user表）
        List<SolutionVO> list = commentMapper.selectByIdsOrdered(ids);

        // 通过Feign批量获取用户名
        if (list != null && !list.isEmpty()) {
            List<Long> userIds = list.stream()
                    .map(SolutionVO::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            List<Long> replyUserIds = list.stream()
                    .map(SolutionVO::getReplyToUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            userIds.addAll(replyUserIds);
            userIds = userIds.stream().distinct().toList();

            if (!userIds.isEmpty()) {
                Result<List<UserFeignDTO>> userResult = userClient.getUsersByIds(userIds);
                if (userResult != null && userResult.getData() != null) {
                    Map<Long, String> nameMap = userResult.getData().stream()
                            .collect(Collectors.toMap(UserFeignDTO::getId, UserFeignDTO::getUsername, (a, b) -> a));
                    list.forEach(vo -> {
                        if (vo.getUserId() != null) {
                            vo.setUsername(nameMap.get(vo.getUserId()));
                        }
                        if (vo.getReplyToUserId() != null) {
                            vo.setReplyToUsername(nameMap.get(vo.getReplyToUserId()));
                        }
                    });
                }
            }

            // Markdown渲染
            list.forEach(solutionVO -> {
                if (solutionVO.getContent() != null) {
                    String contentHtml = renderer.render(parser.parse(solutionVO.getContent()));
                    solutionVO.setContentHtml(contentHtml);
                }
            });
        }

        result.setList(list);
        result.setMinTime(minTime);
        result.setOffset(newOffset);
        return result;
    }
}
