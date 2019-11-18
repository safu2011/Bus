const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp(functions.config().firebase)


exports.ArrivedNotification = functions.database.ref('/Consumers List/{customerId}/Arrived')
    .onCreate((snapshot, context) => {
        arrived = snapshot.val();
        const customerId = context.params.customerId
        console.log("Customer has arrived = ", arrived)

        return admin.database().ref("/Consumers List/"+customerId+"/Messaging Token").once('value')
            .then(snap => {

                const token = snap.val();
                if (token === "null") {
                    console.log("User Offline")
                    return snapshot;
                }
                console.log("token: ", token)

                //we have everything we need
                //Build the message payload and send the message
                console.log("Construction the notification message.")
                const payload = {
                    data: {
                        data_type: "Arrived"

                    }
                }

                return admin.messaging().sendToDevice(token, payload)
                    .then(function (response) {
                        console.log("Successfully sent message:", response);
                        return Response;
                    })
                    .catch(function (error) {
                        console.log("Error sending message:", error);
                        return error;
                    });

            });


    })
