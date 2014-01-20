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

    var gravatar = function(sender) {
        return 'http://www.gravatar.com/avatar/' + CryptoJS.MD5(sender) + '?s=64&r=pg&d=wavatar';
    }
    var newChatLine = function(sender, message, timeStamp) {
        console.log("da new chatline");
        if (!$scope.chat.lines) {
            $scope.chat.lines = Array();
        }
        while ($scope.chat.lines.length > 49) {
            $scope.chat.lines.shift();
        }
        $scope.chat.lines.push({sender: sender, message: message, timeStamp: parseInt(timeStamp), avatar: gravatar(sender)});
        if (lastMessageTimeStamp < timeStamp) {
            lastMessageTimeStamp = timeStamp;
            console.log("new timestamp " + lastMessageTimeStamp);
        }
    };
    var updateToken = function(tokenUpdate) {
        $scope.token.applicants = tokenUpdate.applicants;
        $scope.token.claimedBy = tokenUpdate.claimedBy;
        $scope.token.claimTime = tokenUpdate.claimTime;
        $scope.token.remembered = tokenUpdate.remembered;
        $scope.token.picurl = tokenUpdate.picurl;
        if (lastMessageTimeStamp < tokenUpdate.timeStamp) {
            lastMessageTimeStamp = tokenUpdate.timeStamp;
            console.log("new timestamp " + lastMessageTimeStamp);
        }

    }

    $scope.$watch("chat.tokenBaseUrl", function(){


        (function tick() {
            url = $scope.chat.tokenBaseUrl + lastMessageTimeStamp;
            myService.async(url).then(function(data) {
                if (data["messages"]) {
                    console.log("data messages: " + data["messages"]);
                    data["messages" ].forEach(function (message) {
                        if (message == null) return;
                        console.log("tok");
                        newChatLine(message["sender"], message["message"], message["timeStamp"])
                    })
                }
                if (data["tokenUpdates"]) {
                    data["tokenUpdates" ].forEach(function (tokenUpdate) {
                        if (tokenUpdate == null) return;
                        console.log("tok");
                        updateToken(tokenUpdate);
                    })
                }

                $scope.data = data;
                $timeout(tick, 600);

            },function(response) {
                console.log("error?");
                $timeout(tick, 10000);
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
                if ($(".chatList").css("overflow") == "auto") {
                    var chat_height = $(element)[0].scrollHeight;
                    console.log(chat_height);
                    element.scrollTop(chat_height);
                } else {
                    window.scrollTo(0,document.body.scrollHeight);
                }
            });
        }
    }
});