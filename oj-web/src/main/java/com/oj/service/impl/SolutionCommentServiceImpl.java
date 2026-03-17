package com.oj.service.impl;

import com.oj.context.BaseContext;
import com.oj.dto.CommentDTO;
import com.oj.entity.SolutionComment;
import com.oj.mapper.CommentMapper;
import com.oj.mapper.UserMapper;
import com.oj.result.Result;
import com.oj.service.SolutionCommentService;
import com.oj.utils.MarkdownUtil;
import com.oj.vo.ScrollResult;
import com.oj.vo.SolutionVO;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class SolutionCommentServiceImpl implements SolutionCommentService {
    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private Parser parser; // 注入配置类中的 Bean

    @Autowired
    private HtmlRenderer renderer; // 注入配置类中的 Bean

    private static final String SOLUTION_KEY = "solution:problem:";
    private static final String COMMENT_KEY = "solution:comments:";
    private static final int PAGE_SIZE = 5;
    @Autowired
    private UserMapper userMapper;

    @Override
    public void save(CommentDTO commentDTO) {
        Long currentId = BaseContext.getCurrentId();
        SolutionComment solutionComment=new SolutionComment();
        BeanUtils.copyProperties(commentDTO,solutionComment);
        solutionComment.setUserId(currentId);
        solutionComment.setCreateTime(LocalDateTime.now());
        solutionComment.setUpdateTime(LocalDateTime.now());
        solutionComment.setType(1);
        //保存评论/笔记
        commentMapper.insert(solutionComment);
        String key= SOLUTION_KEY+solutionComment.getProblemId();
        stringRedisTemplate.opsForZSet().add(key,solutionComment.getId().toString(),System.currentTimeMillis());
    }

    @Override
    public Result<ScrollResult> querySolutions(Long problemId, Long lastId, Integer offset) {
        String key = SOLUTION_KEY + problemId;
        return Result.success(scrollQuery(key, lastId, offset));
    }

    @Override
    public Result saveComment(CommentDTO commentDTO) {
        Long currentId = BaseContext.getCurrentId();
        SolutionComment solutionComment=new SolutionComment();
        BeanUtils.copyProperties(commentDTO,solutionComment);
        solutionComment.setUserId(currentId);
        solutionComment.setCreateTime(LocalDateTime.now());
        solutionComment.setUpdateTime(LocalDateTime.now());
        solutionComment.setType(2);
        solutionComment.setLikeCount(0);
        solutionComment.setCommentCount(0);
        solutionComment.setViewCount(0);
        solutionComment.setStatus(1);
        commentMapper.insert(solutionComment);
        String key= COMMENT_KEY+solutionComment.getProblemId();
        stringRedisTemplate.opsForZSet().add(key,solutionComment.getId().toString(),System.currentTimeMillis());
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
        SolutionVO solutionVO=new SolutionVO();
        BeanUtils.copyProperties(solutionComment,solutionVO);
        MarkdownUtil.parseMarkdownField(solutionVO, "content", "contentHtml", parser, renderer);
        solutionComment.setViewCount(solutionComment.getViewCount()+1);
        commentMapper.updateById(solutionComment);
        solutionVO.setUsername(userMapper.selectById(solutionVO.getUserId()).getUsername());
        return Result.success(solutionVO);
    }

    /**
     * 通用滚动分页
     */
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

        // 按ID顺序查出完整数据（带用户名）
        List<SolutionVO> list = commentMapper.selectVOByIdsOrdered(ids);
        //获取markdown文本进行渲染
        if(list!=null){
            List<SolutionVO> solutionVOS = list.stream().map(solutionVO -> {
                MarkdownUtil.parseMarkdownField(solutionVO, "content", "contentHtml", parser, renderer);
                return solutionVO;
            }).toList();
        }
        result.setList(list);
        result.setMinTime(minTime);
        result.setOffset(newOffset);
        return result;
    }
}
