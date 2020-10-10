# Auction House Charts

[![Build Status](https://travis-ci.org/GuillaumeEveillard/ah-charts.svg?branch=master)](https://travis-ci.org/GuillaumeEveillard/ah-charts)

Little project written in Kotlin in Typescript used to monitor item prices in WoW Classic.

## How does it work?

- The data are extracted from Lua files generated by two WoW add-ons: TSM and Auctionator
- The Kotlin server stores the meaningful data, computes some statistics and provides a REST API
- The front end (written in Typescript) call the REST API and draws charts using chart.js
