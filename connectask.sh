

REGION=ap-southeast-1
CLUSTER_NAME=arn:aws:ecs:ap-southeast-1:940445445185:cluster/demo-laravel-idp-alb
TASKID=fd3de807b83b4375ae95daf32d215992
CONTAINER_NAME=demo-laravel-idp
SERVICE=demo-laravel-idp-alb

#aws ecs update-service --cluster $CLUSTER_NAME --service $SERVICE --force-new-deployment --enable-execute-command

aws ecs execute-command \
--region $REGION \
--cluster $CLUSTER_NAME \
--task $TASKID \
--container $CONTAINER_NAME \
--interactive \
--command "/bin/sh" \
--debug 

#cd storage/logs | /var/log/apache2