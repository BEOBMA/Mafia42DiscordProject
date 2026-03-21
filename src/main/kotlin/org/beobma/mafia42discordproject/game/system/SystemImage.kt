package org.beobma.mafia42discordproject.game.system

enum class SystemImage(val imageUrl: String) {
    // 밤/낮 전환 및 안내 이미지
    DAY_START("https://cdn.discordapp.com/.../7aace941ae58a6cc.png..."),
    NIGHT_START("https://cdn.discordapp.com/.../43e6c3860a090af9.png..."),

    // 투표 관련 이미지
    VOTE_START("https://cdn.discordapp.com/.../bd6d8d833d736bf2.png..."),
    DEFENSE_START("https://cdn.discordapp.com/.../b1bb8f82a19e45e3.png..."),
    VOTING_FAILURE("https://cdn.discordapp.com/attachments/1483977619258212392/1484594233653465122/K5WjViOFIiajx3YUfctCF-wkTWwg-DnerBQ09EXEd5-Jxz6Yy0vAmAuM5XDOMIWqHpYOXk85dCobA6CkwzPxOILsPNTbKJgtpYa1DtnVqhceybFNoLK5kdEtPJr6x7rCpn5F3Au_wTeTK0zWtRNArQ.webp?ex=69becb9f&is=69bd7a1f&hm=95cc33354d29bf53d2a74db6ca5ac622b88ef11bfe5b9e419f6e7b38a6f2a8b4&"),
    VOTE_EXECUTION("https://cdn.discordapp.com/attachments/1483977619258212392/1484594233288691895/22SIfKIG4sgmfsgKpScS00MYCCNg70dZoYW9wB3zjuIlnN7d56sqkmFViOFPYrPnPJixJ-BEj5f_mVUp2wcYAzYpHKjyZDuoQyzfp3efnGqc1UYKkMLrk0w5QxCV5tlorhBipi2-c69B7eSYhppyIA.webp?ex=69becb9f&is=69bd7a1f&hm=0b3d5473bbaebb91f2335ef3d07cf315043fde1889930328ea4211c486e792df&"),

    // 사망 및 결과 이미지
    QUIET_NIGHT("https://cdn.discordapp.com/attachments/1483977619258212392/1483980003015397446/d8692f78c3528f76.png?ex=69bc8f93&is=69bb3e13&hm=1378e1b6daba26baddf0cc5d042087b7c5151860d709a3140414b97f774b77a4&"),
    DEATH_BY_MAFIA("https://cdn.discordapp.com/attachments/1483977619258212392/1483980246448603146/99cb963d1b44dc2e.png?ex=69bc8fcd&is=69bb3e4d&hm=51de46f9128d899572989dc0deb0717d66fd93097e5feac91386e9db0901461d&"),
    DEATH_BY_LYNCH("https://cdn.discordapp.com/.../22SIfKIG4sgmfsgKpScS00MY..."),
    LYNCH_FAILED("https://cdn.discordapp.com/.../K5WjViOFIiajx3YUfctCF..."),

    // 확장: 추후 독살, 폭사 등 추가 가능

}