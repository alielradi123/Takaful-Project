const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onDonationDistributed = functions.firestore
  .document("donations/{donationId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    // نتحقق مما إذا كانت حالة التبرع قد تغيرت لتصبح "تم التوزيع"
    if (beforeData.status !== "تم التوزيع" && afterData.status === "تم التوزيع") {
      const caseId = afterData.caseId;
      if (!caseId) return null;

      // 1. جلب بيانات الحالة (Case) لمعرفة صاحبها (المستفيد)
      const caseSnapshot = await admin.firestore().collection("cases").doc(caseId).get();
      if (!caseSnapshot.exists) return null;
      
      const caseData = caseSnapshot.data();
      const beneficiaryId = caseData.userId; // نفترض أن الحالة تحتوي على userId لصاحبها
      if (!beneficiaryId) return null;

      // 2. جلب الـ FCM Token الخاص بالمستفيد من مجموعة Users
      const userSnapshot = await admin.firestore().collection("users").doc(beneficiaryId).get();
      if (!userSnapshot.exists) return null;

      const userData = userSnapshot.data();
      const fcmToken = userData.fcmToken;
      if (!fcmToken) {
          console.log(`لم يتم العثور على توكن FCM للمستخدم ${beneficiaryId}`);
          return null;
      }

      // 3. إرسال إشعار الدفع (Push Notification)
      const payload = {
        notification: {
          title: "تكافل - تحديث الحالة 💙",
          body: `بشرى! تم توزيع تبرع جديد لحالتك (${caseData.title}) بقيمة ${afterData.amount} ريال.`
        },
        token: fcmToken
      };

      try {
        await admin.messaging().send(payload);
        console.log(`تم إرسال الإشعار بنجاح للمستخدم ${beneficiaryId}`);
      } catch (error) {
        console.error("حدث خطأ أثناء إرسال الإشعار:", error);
      }
    }
    return null;
  });
