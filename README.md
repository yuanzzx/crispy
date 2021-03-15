# crispy
基于 raft 算法实现的强一致性kv缓存

# 请求方式
新增  
curl -X POST localhost:8111/keys -H Content-Type:application/json -d {\"key\":\"234\",\"value\":\"123\"}

查询  
curl -X GET localhost:8111/keys?key=234 -H Content-Type:application/json

修改  
curl -X PUT localhost:8111/keys -H Content-Type:application/json -d {\"key\":\"234\",\"value\":\"234\"}

删除      
curl -X DELETE localhost:8111/keys -H Content-Type:application/json -d {\"key\":\"234\"}
