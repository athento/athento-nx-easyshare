<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <title>
     <@block name="title">
     Athento - EasyShare
     </@block>
  </title>
  <meta name="description" content="Athento Easy Share Folder">
  <meta name="viewport" content="width=device-width">
  <link href='https://fonts.googleapis.com/css?family=Lato' rel='stylesheet' type='text/css'>
  <link rel="stylesheet" href="${skinPath}/css/normalize.css">
  <link rel="stylesheet" href="${skinPath}/css/site.css">
</head>

<body>
  <section class="easyshare">
    <div class="container">
      <div class="logo">
        <img src="${skinPath}/img/logo_athento.png" width="168"/>
      </div>
      <div class="wrapper">
          <main class="share-box">
            <@block name="content">The Content</@block>
          </main>
      </div>
    </div>
  </section>
</body>
</html>