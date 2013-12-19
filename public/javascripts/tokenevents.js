var app = angular.module('token', ['ngResource']);


app.factory('myService', function($http) {
    var myService = {
        async: function(url) {

            // $http returns a promise, which has a then function, which also returns a promise
            var promise = $http.get(url).then(function (response) {
                // The then function here is an opportunity to modify the response
                console.log("got " + url);
                console.log(response);
                // The return value gets picked up by the then in the controller.
                return response.data;
            });
            // Return the promise to the controller
            return promise;
        }
    }


    return myService;
});

app.controller('TokenChatCtrl', function( myService,$scope, $timeout) {

    var lastMessageTimeStamp = 0;
    var newChatLine = function(sender, message, timeStamp) {
        console.log("da new chatline");
        if (!$scope.chat.lines) {
            $scope.chat.lines = Array();
        }
        $scope.chat.lines.push({sender: sender, message: message, timeStamp: timeStamp});
        if (lastMessageTimeStamp < timeStamp) {
            lastMessageTimeStamp = timeStamp;
            console.log("new timestamp " + lastMessageTimeStamp);
        }
    };

    $scope.$watch("chat.tokenBaseUrl", function(){


        (function tick() {
            url = $scope.chat.tokenBaseUrl + lastMessageTimeStamp;
            myService.async(url).then(function(data) {
                if (data["messages"]) {
                    console.log("data messages: " + data["messages"]);
                    data["messages" ].forEach(function (message) {
                        console.log("tok");
                        newChatLine(message["sender"], message["message"], message["timeStamp"])
                    })
                }

                $scope.data = data;
                $timeout(tick, 1000);

            });
        })();
    });
});



app.directive('scrollItem',function(){
    return{
        restrict: "A",
        link: function(scope, element, attributes) {
            if (scope.$last){ // If this is the last item, trigger an event
                scope.$emit("Finished");
            }
        }
    }
});

app.directive('scrollIf', function() {
    return{
        restrict: "A",
        link: function(scope, element, attributes) {
            scope.$on("Finished",function(){ //Handle an event when all the items are rendered with ng-repeat
                var chat_height = $(element)[0].scrollHeight;
                console.log(chat_height);
                element.scrollTop(chat_height);
            });
        }
    }
});