#! /bin/bash

#
# This script creates the topics and subscriptions for the pubsub emulator.
#
# To avoid race conditions this script starts by waiting for the pubsub emulator to start.
# If the pubsub emulator is not running then the script will block at this point.
#

set -e

PUBSUB_HOST=localhost
PUBSUB_PORT=9808
PUBSUB_PROJECT=local


function createTopic {
  TOPIC_NAME=$1 
  
  curl -X PUT http://$PUBSUB_HOST:$PUBSUB_PORT/v1/projects/$PUBSUB_PROJECT/topics/$TOPIC_NAME
} 

function createSubscription {
  TOPIC_NAME=$1 
  SUBSCRIPTION_NAME=$2

  SUBSCRIPTION_URL=http://$PUBSUB_HOST:$PUBSUB_PORT/v1/projects/$PUBSUB_PROJECT/subscriptions/$SUBSCRIPTION_NAME
  TOPIC_REF="projects/$PUBSUB_PROJECT/topics/$TOPIC_NAME"

  curl -X PUT -H 'Content-Type: application/json' $SUBSCRIPTION_URL -d '{"topic": "'$TOPIC_REF'"}'
}


echo "<<< Waiting for pubsub emulator to start"
bash -c 'while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' '$PUBSUB_HOST:$PUBSUB_PORT')" != "200" ]]; do sleep 1; done'

echo "<<< Creating topics >>>"
createTopic event_case-update
createTopic event_uac-update
createTopic event_survey-update
createTopic event_collection-exercise-update
createTopic event_refusal
createTopic event_eq-launch

echo "<<< Creating subscriptions >>>"
createSubscription event_case-update event_case-update_cc
createSubscription event_uac-update event_uac-update_cc
createSubscription event_survey-update event_survey-update_cc
createSubscription event_collection-exercise-update event_collection-exercise-update_cc
createSubscription event_refusal event_refusal_cc
