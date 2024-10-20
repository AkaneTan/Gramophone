package org.akanework.gramophone

import org.akanework.gramophone.logic.utils.SemanticLyrics.LyricLine

object LrcTestData {
	const val AS_IT_WAS = "[00:00.29]Come on, Harry, we wanna say \"good night\" to you!.\n[00:04.45].\n[00:14.23]Holding me back.\n[00:16.25]Gravity's holding me back.\n[00:18.74]I want you to hold out the palm of your hand.\n[00:21.88]Why don't we leave it at that?.\n[00:25.13]Nothing to say.\n[00:27.12]When everything gets in the way.\n[00:29.84]Seems you cannot be replaced.\n[00:32.80]And I'm the one who will stay.\n[00:34.98]Oh-oh-oh.\n[00:37.67]In this world.\n[00:40.42]It's just us.\n[00:44.23]You know it's not the same as it was.\n[00:48.82]In this world.\n[00:51.51]It's just us.\n[00:55.29]You know it's not the same as it was.\n[01:00.21]As it was.\n[01:03.03]As it was.\n[01:06.37]You know it's not the same.\n[01:09.35]Answer the phone.\n[01:11.52]Harry, you're no good alone.\n[01:14.12]Why are you sitting at home on the floor?.\n[01:16.99]What kind of pills are you on?.\n[01:20.29]Ringing the bell.\n[01:22.30]And nobody's coming to help.\n[01:25.23]Your daddy lives by himself.\n[01:27.66]He just wants to know that you're well.\n[01:30.26]Oh-oh-oh.\n[01:32.99]In this world.\n[01:35.62]It's just us.\n[01:39.28]You know it's not the same as it was.\n[01:43.98]In this world.\n[01:46.66]It's just us.\n[01:50.33]You know it's not the same as it was.\n[01:55.43]As it was.\n[01:58.24]As it was.\n[02:01.37]You know it's not the same.\n[02:04.55]Go home, get ahead, light-speed internet.\n[02:07.39]I don't wanna talk about the way that it was.\n[02:10.18]Leave America, two kids follow her.\n[02:12.93]I don't wanna talk about who's doing it first.\n[02:18.06]Hey!.\n[02:23.00]As it was.\n[02:26.37]You know it's not the same as it was.\n[02:31.38]As it was.\n[02:33.91]As it was.\n[02:35.10]"
	val AS_IT_WAS_PARSED = listOf(
		Pair(LyricLine(start = 290uL, text = """Come on, Harry, we wanna say "good night" to you!.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 4450uL, text = """.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 14230uL, text = """Holding me back.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 16250uL, text = """Gravity's holding me back.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 18740uL, text = """I want you to hold out the palm of your hand.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 21880uL, text = """Why don't we leave it at that?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 25130uL, text = """Nothing to say.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 27120uL, text = """When everything gets in the way.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 29840uL, text = """Seems you cannot be replaced.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 32800uL, text = """And I'm the one who will stay.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 34980uL, text = """Oh-oh-oh.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 37670uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 40420uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 44230uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 48820uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 51510uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 55290uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 60210uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 63030uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 66370uL, text = """You know it's not the same.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 69350uL, text = """Answer the phone.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 71520uL, text = """Harry, you're no good alone.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 74120uL, text = """Why are you sitting at home on the floor?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 76990uL, text = """What kind of pills are you on?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 80290uL, text = """Ringing the bell.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 82300uL, text = """And nobody's coming to help.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 85230uL, text = """Your daddy lives by himself.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 87660uL, text = """He just wants to know that you're well.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 90260uL, text = """Oh-oh-oh.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 92990uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 95620uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 99280uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 103980uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 106660uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 110330uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 115430uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 118240uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 121370uL, text = """You know it's not the same.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 124550uL, text = """Go home, get ahead, light-speed internet.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 127390uL, text = """I don't wanna talk about the way that it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 130180uL, text = """Leave America, two kids follow her.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 132930uL, text = """I don't wanna talk about who's doing it first.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 138060uL, text = """Hey!.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 143000uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 146370uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 151380uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 153910uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 155100uL, text = """""", words = null, speaker = null), false),
	)
	const val AS_IT_WAS_PARSED_STR = """val testData = listOf(
	Pair(LyricLine(start = 290uL, text = ""${'"'}Come on, Harry, we wanna say "good night" to you!.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 4450uL, text = ""${'"'}.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 14230uL, text = ""${'"'}Holding me back.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 16250uL, text = ""${'"'}Gravity's holding me back.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 18740uL, text = ""${'"'}I want you to hold out the palm of your hand.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 21880uL, text = ""${'"'}Why don't we leave it at that?.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 25130uL, text = ""${'"'}Nothing to say.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 27120uL, text = ""${'"'}When everything gets in the way.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 29840uL, text = ""${'"'}Seems you cannot be replaced.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 32800uL, text = ""${'"'}And I'm the one who will stay.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 34980uL, text = ""${'"'}Oh-oh-oh.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 37670uL, text = ""${'"'}In this world.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 40420uL, text = ""${'"'}It's just us.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 44230uL, text = ""${'"'}You know it's not the same as it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 48820uL, text = ""${'"'}In this world.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 51510uL, text = ""${'"'}It's just us.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 55290uL, text = ""${'"'}You know it's not the same as it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 60210uL, text = ""${'"'}As it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 63030uL, text = ""${'"'}As it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 66370uL, text = ""${'"'}You know it's not the same.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 69350uL, text = ""${'"'}Answer the phone.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 71520uL, text = ""${'"'}Harry, you're no good alone.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 74120uL, text = ""${'"'}Why are you sitting at home on the floor?.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 76990uL, text = ""${'"'}What kind of pills are you on?.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 80290uL, text = ""${'"'}Ringing the bell.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 82300uL, text = ""${'"'}And nobody's coming to help.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 85230uL, text = ""${'"'}Your daddy lives by himself.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 87660uL, text = ""${'"'}He just wants to know that you're well.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 90260uL, text = ""${'"'}Oh-oh-oh.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 92990uL, text = ""${'"'}In this world.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 95620uL, text = ""${'"'}It's just us.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 99280uL, text = ""${'"'}You know it's not the same as it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 103980uL, text = ""${'"'}In this world.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 106660uL, text = ""${'"'}It's just us.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 110330uL, text = ""${'"'}You know it's not the same as it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 115430uL, text = ""${'"'}As it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 118240uL, text = ""${'"'}As it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 121370uL, text = ""${'"'}You know it's not the same.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 124550uL, text = ""${'"'}Go home, get ahead, light-speed internet.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 127390uL, text = ""${'"'}I don't wanna talk about the way that it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 130180uL, text = ""${'"'}Leave America, two kids follow her.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 132930uL, text = ""${'"'}I don't wanna talk about who's doing it first.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 138060uL, text = ""${'"'}Hey!.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 143000uL, text = ""${'"'}As it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 146370uL, text = ""${'"'}You know it's not the same as it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 151380uL, text = ""${'"'}As it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 153910uL, text = ""${'"'}As it was.""${'"'}, words = null, speaker = null), false),
	Pair(LyricLine(start = 155100uL, text = ""${'"'}${'"'}${'"'}${'"'}, words = null, speaker = null), false),
)
"""
	const val AS_IT_WAS_NO_TRIM = "[00:00.29]Come on, Harry, we wanna say \"good night\" to you!.\n[00:04.45] .\n[00:14.23]Holding me back.\n[00:16.25]Gravity's holding me back.\n[00:18.74]I want you to hold out the palm of your hand.\n[00:21.88]Why don't we leave it at that?.\n[00:25.13]Nothing to say.\n[00:27.12]When everything gets in the way.\n[00:29.84]Seems you cannot be replaced.\n[00:32.80]And I'm the one who will stay.\n[00:34.98]Oh-oh-oh.\n[00:37.67]In this world.\n[00:40.42]It's just us.\n[00:44.23]You know it's not the same as it was.\n[00:48.82]In this world.\n[00:51.51]It's just us.\n[00:55.29]You know it's not the same as it was.\n[01:00.21]As it was.\n[01:03.03]As it was.\n[01:06.37]You know it's not the same.\n[01:09.35]Answer the phone.\n[01:11.52]Harry, you're no good alone.\n[01:14.12]Why are you sitting at home on the floor?.\n[01:16.99]What kind of pills are you on?.\n[01:20.29]Ringing the bell.\n[01:22.30]And nobody's coming to help.\n[01:25.23]Your daddy lives by himself.\n[01:27.66]He just wants to know that you're well.\n[01:30.26]Oh-oh-oh.\n[01:32.99]In this world.\n[01:35.62]It's just us.\n[01:39.28]You know it's not the same as it was.\n[01:43.98]In this world.\n[01:46.66]It's just us.\n[01:50.33]You know it's not the same as it was.\n[01:55.43]As it was.\n[01:58.24]As it was.\n[02:01.37]You know it's not the same.\n[02:04.55]Go home, get ahead, light-speed internet.\n[02:07.39]I don't wanna talk about the way that it was.\n[02:10.18]Leave America, two kids follow her.\n[02:12.93]I don't wanna talk about who's doing it first.\n[02:18.06]Hey!.\n[02:23.00]As it was.\n[02:26.37]You know it's not the same as it was.\n[02:31.38]As it was.\n[02:33.91]As it was.\n[02:35.10]"
	val AS_IT_WAS_NO_TRIM_PARSED_FALSE = listOf(
		Pair(LyricLine(start = 290uL, text = """Come on, Harry, we wanna say "good night" to you!.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 4450uL, text = """ .""", words = null, speaker = null), false),
		Pair(LyricLine(start = 14230uL, text = """Holding me back.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 16250uL, text = """Gravity's holding me back.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 18740uL, text = """I want you to hold out the palm of your hand.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 21880uL, text = """Why don't we leave it at that?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 25130uL, text = """Nothing to say.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 27120uL, text = """When everything gets in the way.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 29840uL, text = """Seems you cannot be replaced.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 32800uL, text = """And I'm the one who will stay.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 34980uL, text = """Oh-oh-oh.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 37670uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 40420uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 44230uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 48820uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 51510uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 55290uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 60210uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 63030uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 66370uL, text = """You know it's not the same.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 69350uL, text = """Answer the phone.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 71520uL, text = """Harry, you're no good alone.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 74120uL, text = """Why are you sitting at home on the floor?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 76990uL, text = """What kind of pills are you on?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 80290uL, text = """Ringing the bell.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 82300uL, text = """And nobody's coming to help.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 85230uL, text = """Your daddy lives by himself.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 87660uL, text = """He just wants to know that you're well.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 90260uL, text = """Oh-oh-oh.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 92990uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 95620uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 99280uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 103980uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 106660uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 110330uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 115430uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 118240uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 121370uL, text = """You know it's not the same.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 124550uL, text = """Go home, get ahead, light-speed internet.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 127390uL, text = """I don't wanna talk about the way that it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 130180uL, text = """Leave America, two kids follow her.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 132930uL, text = """I don't wanna talk about who's doing it first.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 138060uL, text = """Hey!.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 143000uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 146370uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 151380uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 153910uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 155100uL, text = """""", words = null, speaker = null), false),
	)
	val AS_IT_WAS_NO_TRIM_PARSED_TRUE = listOf(
		Pair(LyricLine(start = 290uL, text = """Come on, Harry, we wanna say "good night" to you!.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 4450uL, text = """.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 14230uL, text = """Holding me back.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 16250uL, text = """Gravity's holding me back.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 18740uL, text = """I want you to hold out the palm of your hand.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 21880uL, text = """Why don't we leave it at that?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 25130uL, text = """Nothing to say.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 27120uL, text = """When everything gets in the way.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 29840uL, text = """Seems you cannot be replaced.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 32800uL, text = """And I'm the one who will stay.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 34980uL, text = """Oh-oh-oh.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 37670uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 40420uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 44230uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 48820uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 51510uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 55290uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 60210uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 63030uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 66370uL, text = """You know it's not the same.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 69350uL, text = """Answer the phone.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 71520uL, text = """Harry, you're no good alone.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 74120uL, text = """Why are you sitting at home on the floor?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 76990uL, text = """What kind of pills are you on?.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 80290uL, text = """Ringing the bell.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 82300uL, text = """And nobody's coming to help.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 85230uL, text = """Your daddy lives by himself.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 87660uL, text = """He just wants to know that you're well.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 90260uL, text = """Oh-oh-oh.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 92990uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 95620uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 99280uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 103980uL, text = """In this world.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 106660uL, text = """It's just us.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 110330uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 115430uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 118240uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 121370uL, text = """You know it's not the same.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 124550uL, text = """Go home, get ahead, light-speed internet.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 127390uL, text = """I don't wanna talk about the way that it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 130180uL, text = """Leave America, two kids follow her.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 132930uL, text = """I don't wanna talk about who's doing it first.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 138060uL, text = """Hey!.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 143000uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 146370uL, text = """You know it's not the same as it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 151380uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 153910uL, text = """As it was.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 155100uL, text = """""", words = null, speaker = null), false),
	)
	const val DREAM_THREAD = "[00:00.00][00:20.96][00:54.53][01:15.51][01:38.51][01:59.64][02:41.98][02:52.02][03:09.80]\n" +
			"[00:00.83]そっと目を覚ませば　暗い闇に覆われ\n" +
			"[00:00.83]Quietly, when I open my eyes, I find myself covered in dark darkness\n" +
			"[00:10.79]1人…また1人…散る涙に悲しみを堪えて\n" +
			"[00:10.79]Alone... one by one... holding back the sadness in falling tears\n" +
			"[00:18.31]手に掴むと誓った　希望を\n" +
			"[00:18.31]I pledged to grasp onto hope\n" +
			"[00:35.27]ずっと夢を見ていた　遠く高く届かない\n" +
			"[00:35.27]I've always dreamed, unreachable, far and high\n" +
			"[00:44.97]きっと叶うんだってそう願ってた\n" +
			"[00:44.97]Surely it will come true, that's what I wished for\n" +
			"[00:50.21]そんな時に君が私と繋がった\n" +
			"[00:50.21]At that moment, you became connected to me\n" +
			"[00:54.66][02:42.37]“いつだって前向きで”　“いつだって純粋で”\n" +
			"[00:54.66][02:42.37]\"Always be positive\" \"Always be pure\"\n" +
			"[01:00.05][02:47.83]きっと君とならいけるよ\n" +
			"[01:00.05][02:47.83]I'm sure I can do it with you\n" +
			"[01:04.40]必ずこの世界を光の園へ\n" +
			"[01:04.40]Definitely, I'll lead this world to the garden of light\n" +
			"[01:09.90][01:53.96]絶対変えてみせる　未来へと繋ぐよ\n" +
			"[01:09.90][01:53.96]I'll show you, connecting to the future\n" +
			"[01:19.39]ずっと夢に魅せられ　想い胸に絡まる\n" +
			"[01:19.39]Enchanted by dreams all along, feelings entwined in my heart\n" +
			"[01:29.12]強く想う程に湧き上がってく\n" +
			"[01:29.12]The more strongly I feel, the more it wells up\n" +
			"[01:34.28]まるで“夢の糸”に絡まれたみたいに\n" +
			"[01:34.28]As if entangled in the \"threads of dreams\"\n" +
			"[01:38.72]いつだって追い求め　いつだって熱い鼓動を\n" +
			"[01:38.72]Always pursuing, always with a passionate heartbeat\n" +
			"[01:44.10]ずっと燃やし歩んでいく\n" +
			"[01:44.10]I'll keep burning and walking forever\n" +
			"[01:48.52]溢れるこの想いよ　あなたに届け\n" +
			"[01:48.52]Let these overflowing feelings reach you\n" +
			"[02:17.93]夢の糸　囚われた私は蝶となり\n" +
			"[02:17.93]Threads of dreams, I, who was imprisoned, become a butterfly\n" +
			"[02:28.06]あの空の下へと帰るから\n" +
			"[02:28.06]Because I will return beneath that sky\n" +
			"[02:34.95]いつの日かこの世界を塗り替えよう\n" +
			"[02:34.95]Someday, let's repaint this world\n" +
			"[02:52.14]絡まる糸　夢を引きよせる魔法\n" +
			"[02:52.14]Entwined threads, a magic that pulls dreams\n" +
			"[02:57.64]絶対変えてみせる　未来へと\n" +
			"[02:57.64]I'll definitely change and connect to the future\n" +
			"[03:02.76]絶対最高の未来が待ってるよ\n" +
			"[03:02.76]The absolute best future is waiting\n" +
			"[03:06.76]繋げよう\n" +
			"[03:06.76]Let's connect\n"
	val DREAM_THREAD_PARSED = listOf(
		Pair(LyricLine(start = 830uL, text = """そっと目を覚ませば　暗い闇に覆われ""", words = null, speaker = null), false),
		Pair(LyricLine(start = 830uL, text = """Quietly, when I open my eyes, I find myself covered in dark darkness""", words = null, speaker = null), true),
		Pair(LyricLine(start = 10790uL, text = """1人…また1人…散る涙に悲しみを堪えて""", words = null, speaker = null), false),
		Pair(LyricLine(start = 10790uL, text = """Alone... one by one... holding back the sadness in falling tears""", words = null, speaker = null), true),
		Pair(LyricLine(start = 18310uL, text = """手に掴むと誓った　希望を""", words = null, speaker = null), false),
		Pair(LyricLine(start = 18310uL, text = """I pledged to grasp onto hope""", words = null, speaker = null), true),
		Pair(LyricLine(start = 20960uL, text = """""", words = null, speaker = null), false),
		Pair(LyricLine(start = 35270uL, text = """ずっと夢を見ていた　遠く高く届かない""", words = null, speaker = null), false),
		Pair(LyricLine(start = 35270uL, text = """I've always dreamed, unreachable, far and high""", words = null, speaker = null), true),
		Pair(LyricLine(start = 44970uL, text = """きっと叶うんだってそう願ってた""", words = null, speaker = null), false),
		Pair(LyricLine(start = 44970uL, text = """Surely it will come true, that's what I wished for""", words = null, speaker = null), true),
		Pair(LyricLine(start = 50210uL, text = """そんな時に君が私と繋がった""", words = null, speaker = null), false),
		Pair(LyricLine(start = 50210uL, text = """At that moment, you became connected to me""", words = null, speaker = null), true),
		Pair(LyricLine(start = 54530uL, text = """""", words = null, speaker = null), false),
		Pair(LyricLine(start = 54660uL, text = """“いつだって前向きで”　“いつだって純粋で”""", words = null, speaker = null), false),
		Pair(LyricLine(start = 54660uL, text = """"Always be positive" "Always be pure"""", words = null, speaker = null), true),
		Pair(LyricLine(start = 60050uL, text = """きっと君とならいけるよ""", words = null, speaker = null), false),
		Pair(LyricLine(start = 60050uL, text = """I'm sure I can do it with you""", words = null, speaker = null), true),
		Pair(LyricLine(start = 64400uL, text = """必ずこの世界を光の園へ""", words = null, speaker = null), false),
		Pair(LyricLine(start = 64400uL, text = """Definitely, I'll lead this world to the garden of light""", words = null, speaker = null), true),
		Pair(LyricLine(start = 69900uL, text = """絶対変えてみせる　未来へと繋ぐよ""", words = null, speaker = null), false),
		Pair(LyricLine(start = 69900uL, text = """I'll show you, connecting to the future""", words = null, speaker = null), true),
		Pair(LyricLine(start = 75510uL, text = """""", words = null, speaker = null), false),
		Pair(LyricLine(start = 79390uL, text = """ずっと夢に魅せられ　想い胸に絡まる""", words = null, speaker = null), false),
		Pair(LyricLine(start = 79390uL, text = """Enchanted by dreams all along, feelings entwined in my heart""", words = null, speaker = null), true),
		Pair(LyricLine(start = 89120uL, text = """強く想う程に湧き上がってく""", words = null, speaker = null), false),
		Pair(LyricLine(start = 89120uL, text = """The more strongly I feel, the more it wells up""", words = null, speaker = null), true),
		Pair(LyricLine(start = 94280uL, text = """まるで“夢の糸”に絡まれたみたいに""", words = null, speaker = null), false),
		Pair(LyricLine(start = 94280uL, text = """As if entangled in the "threads of dreams"""", words = null, speaker = null), true),
		Pair(LyricLine(start = 98510uL, text = """""", words = null, speaker = null), false),
		Pair(LyricLine(start = 98720uL, text = """いつだって追い求め　いつだって熱い鼓動を""", words = null, speaker = null), false),
		Pair(LyricLine(start = 98720uL, text = """Always pursuing, always with a passionate heartbeat""", words = null, speaker = null), true),
		Pair(LyricLine(start = 104100uL, text = """ずっと燃やし歩んでいく""", words = null, speaker = null), false),
		Pair(LyricLine(start = 104100uL, text = """I'll keep burning and walking forever""", words = null, speaker = null), true),
		Pair(LyricLine(start = 108520uL, text = """溢れるこの想いよ　あなたに届け""", words = null, speaker = null), false),
		Pair(LyricLine(start = 108520uL, text = """Let these overflowing feelings reach you""", words = null, speaker = null), true),
		Pair(LyricLine(start = 113960uL, text = """絶対変えてみせる　未来へと繋ぐよ""", words = null, speaker = null), false),
		Pair(LyricLine(start = 113960uL, text = """I'll show you, connecting to the future""", words = null, speaker = null), true),
		Pair(LyricLine(start = 119640uL, text = """""", words = null, speaker = null), false),
		Pair(LyricLine(start = 137930uL, text = """夢の糸　囚われた私は蝶となり""", words = null, speaker = null), false),
		Pair(LyricLine(start = 137930uL, text = """Threads of dreams, I, who was imprisoned, become a butterfly""", words = null, speaker = null), true),
		Pair(LyricLine(start = 148060uL, text = """あの空の下へと帰るから""", words = null, speaker = null), false),
		Pair(LyricLine(start = 148060uL, text = """Because I will return beneath that sky""", words = null, speaker = null), true),
		Pair(LyricLine(start = 154950uL, text = """いつの日かこの世界を塗り替えよう""", words = null, speaker = null), false),
		Pair(LyricLine(start = 154950uL, text = """Someday, let's repaint this world""", words = null, speaker = null), true),
		Pair(LyricLine(start = 161980uL, text = """""", words = null, speaker = null), false),
		Pair(LyricLine(start = 162370uL, text = """“いつだって前向きで”　“いつだって純粋で”""", words = null, speaker = null), false),
		Pair(LyricLine(start = 162370uL, text = """"Always be positive" "Always be pure"""", words = null, speaker = null), true),
		Pair(LyricLine(start = 167830uL, text = """きっと君とならいけるよ""", words = null, speaker = null), false),
		Pair(LyricLine(start = 167830uL, text = """I'm sure I can do it with you""", words = null, speaker = null), true),
		Pair(LyricLine(start = 172020uL, text = """""", words = null, speaker = null), false),
		Pair(LyricLine(start = 172140uL, text = """絡まる糸　夢を引きよせる魔法""", words = null, speaker = null), false),
		Pair(LyricLine(start = 172140uL, text = """Entwined threads, a magic that pulls dreams""", words = null, speaker = null), true),
		Pair(LyricLine(start = 177640uL, text = """絶対変えてみせる　未来へと""", words = null, speaker = null), false),
		Pair(LyricLine(start = 177640uL, text = """I'll definitely change and connect to the future""", words = null, speaker = null), true),
		Pair(LyricLine(start = 182760uL, text = """絶対最高の未来が待ってるよ""", words = null, speaker = null), false),
		Pair(LyricLine(start = 182760uL, text = """The absolute best future is waiting""", words = null, speaker = null), true),
		Pair(LyricLine(start = 186760uL, text = """繋げよう""", words = null, speaker = null), false),
		Pair(LyricLine(start = 186760uL, text = """Let's connect""", words = null, speaker = null), true),
		Pair(LyricLine(start = 189800uL, text = """""", words = null, speaker = null), false),
	)
	val ALL_STAR = """[by:Greg Camp]
[00:00.100]Somebody once told me the world is gonna roll me
[00:05.000]I ain't the sharpest tool in the shed
[00:09.000]She was lookin kinda dumb with her finger and her thumb
[00:13.000]In the shape of an "L" on her forehead
[00:18.300]Well, the hits start coming and they don't stop coming
[00:21.600]Head to the rules and ya hit the ground running
[00:24.300]Didn't make sense just to live for fun,
[00:26.300]You're brain gets smart but your head gets dumb
[00:28.600]So much to do so much to see
[00:30.600]So what's wrong with takin the backstreets
[00:32.600]You'll never know if you don't go
[00:35.600]You'll never shine if you don't glow
[00:37.900]Hey now, you're an All Star, get your game on, go play
[00:42.900]Hey now, you're a Rock Star, get the show on, get paid,
[00:46.900]And all that glitters is gold
[00:51.200]Only shootin stars break the mold
[00:56.200]It's a cool place, and they say it gets colder
[00:59.500]You're bundled up now, wait till ya get older.
[01:01.500]But the media men beg to differ
[01:03.500]Judgin by the hole in the satellite picture
[01:05.800]The ice we skate is gettin pretty thin
[01:08.100]The water's gettin warm so you might as well swim
[01:11.100]My world's on fire, how about yours?
[01:13.100]Cuz that's the way i like it and i never get bored
[01:16.100]Hey now, you're an All Star, get your game on, go play
[01:20.800]Hey now, you're a Rock Star, get the show on, get paid,
[01:24.800]And all that glitters is gold
[01:28.800]Only shootin stars break the mold
[01:53.100]Hey now, you're an All Star, get your game on, go play
[01:57.400]Hey now, you're a Rock Star, get the show on, get paid,
[02:02.700]And all that glitters is gold
[02:06.000]Only shootin stars
[02:09.300]Somebody once asked could I spare some change for gas
[02:14.300]I need to get myself away from this place
[02:18.300]I said yep, what a concept, I could use a little fuel myself
[02:23.600]And we could all use a little change
[02:27.900]Well, the hits start coming and they don't stop coming
[02:31.900]Head to the rules and I hit the ground running
[02:33.900]Didn't make sense just to live for fun,
[02:35.900]You're brain gets smart but the head gets dumb
[02:38.200]So much to do so much to see
[02:40.500]So what's wrong with takin the backstreets
[02:43.500]You'll never know if you don't go
[02:45.500]You'll never shine if you don't glow
[02:48.100]Hey now, you're an All Star, get your game on, go play
[02:53.100]Hey now, you're a Rock Star, get the show on, get paid,
[02:57.100]And all that glitters is gold
[03:01.100]Only shootin stars break the mold
[03:06.100]And all that glitters is gold
[03:10.100]Only shootin stars break the mold


[by:丨ONER丨]
[00:00.100]有人曾经告诉我世界将要转动
[00:05.000]我是不太聪明
[00:09.000]她的动作看起来有点愚蠢
[00:13.000]她手指在额头前摆出的“L”的形状
[00:18.300]好了 新的时代正在来临 他们无法阻止
[00:21.600]早已厌烦了老套的规则  奔跑着用双脚撞击地面
[00:24.300]没有意义只是为了好玩
[00:26.300]你的大脑变得又聪明又愚蠢
[00:28.600]那么多事要做 那么多话要说
[00:30.600]让我们代替后街男孩又如何
[00:32.600]你永远不会知道，如果你不去尝试
[00:35.600]如果你没有激情，你永远不会发光
[00:37.900]嘿 现在 你是一个全明星 去打你的比赛吧
[00:42.900]嘿 现在 你是一个摇滚明星 去表演你的节目吧
[00:46.900]发光的都是金子
[00:51.200]你只需要打破常规
[00:56.200]这是个很酷的地方 而且他们说更酷了
[00:59.500]你现在就得穿的暖和 等你老了你就知道了
[01:01.500]但是媒体人却要求不同
[01:03.500]从卫星照片上的洞来看
[01:05.800]我们滑的冰越来越薄了
[01:08.100]水变温暖，所以你不妨游游泳
[01:11.100]我的世界着火了 你呢
[01:13.100]因为这是我喜欢的方式，我从来没有厌倦
[01:16.100]嘿 现在 你是一个全明星 去打你的比赛吧
[01:20.800]嘿 现在 你是一个摇滚明星 去表演你的节目吧
[01:24.800]发光的都是金子
[01:28.800]你只需要打破常规
[01:53.100]嘿 现在 你是一个全明星 去打你的比赛吧
[01:57.400]嘿 现在 你是一个摇滚明星 去表演你的节目吧
[02:02.700]发光的都是金子
[02:06.000]你只需要打破常规
[02:09.300]有人曾问我能不能给点零钱买汽油
[02:14.300]我需要远离这个地方
[02:18.300]我说当然 什么概念 我也可以给自己买一点燃料
[02:23.600]然后我们可以一起用零钱
[02:27.900]好了 新的时代正在来临 他们无法阻止
[02:31.900]早已厌烦了老套的规则  奔跑着用双脚撞击地面
[02:33.900]没有意义只是为了好玩
[02:33.900]没有意义只是为了好玩
[02:35.900]你的大脑变得又聪明又愚蠢
[02:38.200]那么多事要做 那么多话要说
[02:40.500]让我们代替后街男孩又如何
[02:43.500]你永远不会知道，如果你不去尝试
[02:45.500]如果你没有激情，你永远不会发光
[02:48.100]嘿 现在 你是一个全明星 去打你的比赛吧
[02:53.100]嘿 现在 你是一个摇滚明星 去表演你的节目吧
[02:57.100]发光的都是金子
[03:01.100]你只需要打破常规
[03:06.100]发光的都是金子
[03:10.100]你只需要打破常规"""
	val ALL_STAR_PARSED = listOf(
		Pair(LyricLine(start = 100uL, text = """Somebody once told me the world is gonna roll me""", words = null, speaker = null), false),
		Pair(LyricLine(start = 100uL, text = """有人曾经告诉我世界将要转动""", words = null, speaker = null), true),
		Pair(LyricLine(start = 5000uL, text = """I ain't the sharpest tool in the shed""", words = null, speaker = null), false),
		Pair(LyricLine(start = 5000uL, text = """我是不太聪明""", words = null, speaker = null), true),
		Pair(LyricLine(start = 9000uL, text = """She was lookin kinda dumb with her finger and her thumb""", words = null, speaker = null), false),
		Pair(LyricLine(start = 9000uL, text = """她的动作看起来有点愚蠢""", words = null, speaker = null), true),
		Pair(LyricLine(start = 13000uL, text = """In the shape of an "L" on her forehead""", words = null, speaker = null), false),
		Pair(LyricLine(start = 13000uL, text = """她手指在额头前摆出的“L”的形状""", words = null, speaker = null), true),
		Pair(LyricLine(start = 18300uL, text = """Well, the hits start coming and they don't stop coming""", words = null, speaker = null), false),
		Pair(LyricLine(start = 18300uL, text = """好了 新的时代正在来临 他们无法阻止""", words = null, speaker = null), true),
		Pair(LyricLine(start = 21600uL, text = """Head to the rules and ya hit the ground running""", words = null, speaker = null), false),
		Pair(LyricLine(start = 21600uL, text = """早已厌烦了老套的规则  奔跑着用双脚撞击地面""", words = null, speaker = null), true),
		Pair(LyricLine(start = 24300uL, text = """Didn't make sense just to live for fun,""", words = null, speaker = null), false),
		Pair(LyricLine(start = 24300uL, text = """没有意义只是为了好玩""", words = null, speaker = null), true),
		Pair(LyricLine(start = 26300uL, text = """You're brain gets smart but your head gets dumb""", words = null, speaker = null), false),
		Pair(LyricLine(start = 26300uL, text = """你的大脑变得又聪明又愚蠢""", words = null, speaker = null), true),
		Pair(LyricLine(start = 28600uL, text = """So much to do so much to see""", words = null, speaker = null), false),
		Pair(LyricLine(start = 28600uL, text = """那么多事要做 那么多话要说""", words = null, speaker = null), true),
		Pair(LyricLine(start = 30600uL, text = """So what's wrong with takin the backstreets""", words = null, speaker = null), false),
		Pair(LyricLine(start = 30600uL, text = """让我们代替后街男孩又如何""", words = null, speaker = null), true),
		Pair(LyricLine(start = 32600uL, text = """You'll never know if you don't go""", words = null, speaker = null), false),
		Pair(LyricLine(start = 32600uL, text = """你永远不会知道，如果你不去尝试""", words = null, speaker = null), true),
		Pair(LyricLine(start = 35600uL, text = """You'll never shine if you don't glow""", words = null, speaker = null), false),
		Pair(LyricLine(start = 35600uL, text = """如果你没有激情，你永远不会发光""", words = null, speaker = null), true),
		Pair(LyricLine(start = 37900uL, text = """Hey now, you're an All Star, get your game on, go play""", words = null, speaker = null), false),
		Pair(LyricLine(start = 37900uL, text = """嘿 现在 你是一个全明星 去打你的比赛吧""", words = null, speaker = null), true),
		Pair(LyricLine(start = 42900uL, text = """Hey now, you're a Rock Star, get the show on, get paid,""", words = null, speaker = null), false),
		Pair(LyricLine(start = 42900uL, text = """嘿 现在 你是一个摇滚明星 去表演你的节目吧""", words = null, speaker = null), true),
		Pair(LyricLine(start = 46900uL, text = """And all that glitters is gold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 46900uL, text = """发光的都是金子""", words = null, speaker = null), true),
		Pair(LyricLine(start = 51200uL, text = """Only shootin stars break the mold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 51200uL, text = """你只需要打破常规""", words = null, speaker = null), true),
		Pair(LyricLine(start = 56200uL, text = """It's a cool place, and they say it gets colder""", words = null, speaker = null), false),
		Pair(LyricLine(start = 56200uL, text = """这是个很酷的地方 而且他们说更酷了""", words = null, speaker = null), true),
		Pair(LyricLine(start = 59500uL, text = """You're bundled up now, wait till ya get older.""", words = null, speaker = null), false),
		Pair(LyricLine(start = 59500uL, text = """你现在就得穿的暖和 等你老了你就知道了""", words = null, speaker = null), true),
		Pair(LyricLine(start = 61500uL, text = """But the media men beg to differ""", words = null, speaker = null), false),
		Pair(LyricLine(start = 61500uL, text = """但是媒体人却要求不同""", words = null, speaker = null), true),
		Pair(LyricLine(start = 63500uL, text = """Judgin by the hole in the satellite picture""", words = null, speaker = null), false),
		Pair(LyricLine(start = 63500uL, text = """从卫星照片上的洞来看""", words = null, speaker = null), true),
		Pair(LyricLine(start = 65800uL, text = """The ice we skate is gettin pretty thin""", words = null, speaker = null), false),
		Pair(LyricLine(start = 65800uL, text = """我们滑的冰越来越薄了""", words = null, speaker = null), true),
		Pair(LyricLine(start = 68100uL, text = """The water's gettin warm so you might as well swim""", words = null, speaker = null), false),
		Pair(LyricLine(start = 68100uL, text = """水变温暖，所以你不妨游游泳""", words = null, speaker = null), true),
		Pair(LyricLine(start = 71100uL, text = """My world's on fire, how about yours?""", words = null, speaker = null), false),
		Pair(LyricLine(start = 71100uL, text = """我的世界着火了 你呢""", words = null, speaker = null), true),
		Pair(LyricLine(start = 73100uL, text = """Cuz that's the way i like it and i never get bored""", words = null, speaker = null), false),
		Pair(LyricLine(start = 73100uL, text = """因为这是我喜欢的方式，我从来没有厌倦""", words = null, speaker = null), true),
		Pair(LyricLine(start = 76100uL, text = """Hey now, you're an All Star, get your game on, go play""", words = null, speaker = null), false),
		Pair(LyricLine(start = 76100uL, text = """嘿 现在 你是一个全明星 去打你的比赛吧""", words = null, speaker = null), true),
		Pair(LyricLine(start = 80800uL, text = """Hey now, you're a Rock Star, get the show on, get paid,""", words = null, speaker = null), false),
		Pair(LyricLine(start = 80800uL, text = """嘿 现在 你是一个摇滚明星 去表演你的节目吧""", words = null, speaker = null), true),
		Pair(LyricLine(start = 84800uL, text = """And all that glitters is gold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 84800uL, text = """发光的都是金子""", words = null, speaker = null), true),
		Pair(LyricLine(start = 88800uL, text = """Only shootin stars break the mold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 88800uL, text = """你只需要打破常规""", words = null, speaker = null), true),
		Pair(LyricLine(start = 113100uL, text = """Hey now, you're an All Star, get your game on, go play""", words = null, speaker = null), false),
		Pair(LyricLine(start = 113100uL, text = """嘿 现在 你是一个全明星 去打你的比赛吧""", words = null, speaker = null), true),
		Pair(LyricLine(start = 117400uL, text = """Hey now, you're a Rock Star, get the show on, get paid,""", words = null, speaker = null), false),
		Pair(LyricLine(start = 117400uL, text = """嘿 现在 你是一个摇滚明星 去表演你的节目吧""", words = null, speaker = null), true),
		Pair(LyricLine(start = 122700uL, text = """And all that glitters is gold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 122700uL, text = """发光的都是金子""", words = null, speaker = null), true),
		Pair(LyricLine(start = 126000uL, text = """Only shootin stars""", words = null, speaker = null), false),
		Pair(LyricLine(start = 126000uL, text = """你只需要打破常规""", words = null, speaker = null), true),
		Pair(LyricLine(start = 129300uL, text = """Somebody once asked could I spare some change for gas""", words = null, speaker = null), false),
		Pair(LyricLine(start = 129300uL, text = """有人曾问我能不能给点零钱买汽油""", words = null, speaker = null), true),
		Pair(LyricLine(start = 134300uL, text = """I need to get myself away from this place""", words = null, speaker = null), false),
		Pair(LyricLine(start = 134300uL, text = """我需要远离这个地方""", words = null, speaker = null), true),
		Pair(LyricLine(start = 138300uL, text = """I said yep, what a concept, I could use a little fuel myself""", words = null, speaker = null), false),
		Pair(LyricLine(start = 138300uL, text = """我说当然 什么概念 我也可以给自己买一点燃料""", words = null, speaker = null), true),
		Pair(LyricLine(start = 143600uL, text = """And we could all use a little change""", words = null, speaker = null), false),
		Pair(LyricLine(start = 143600uL, text = """然后我们可以一起用零钱""", words = null, speaker = null), true),
		Pair(LyricLine(start = 147900uL, text = """Well, the hits start coming and they don't stop coming""", words = null, speaker = null), false),
		Pair(LyricLine(start = 147900uL, text = """好了 新的时代正在来临 他们无法阻止""", words = null, speaker = null), true),
		Pair(LyricLine(start = 151900uL, text = """Head to the rules and I hit the ground running""", words = null, speaker = null), false),
		Pair(LyricLine(start = 151900uL, text = """早已厌烦了老套的规则  奔跑着用双脚撞击地面""", words = null, speaker = null), true),
		Pair(LyricLine(start = 153900uL, text = """Didn't make sense just to live for fun,""", words = null, speaker = null), false),
		Pair(LyricLine(start = 153900uL, text = """没有意义只是为了好玩""", words = null, speaker = null), true),
		Pair(LyricLine(start = 153900uL, text = """没有意义只是为了好玩""", words = null, speaker = null), true),
		Pair(LyricLine(start = 155900uL, text = """You're brain gets smart but the head gets dumb""", words = null, speaker = null), false),
		Pair(LyricLine(start = 155900uL, text = """你的大脑变得又聪明又愚蠢""", words = null, speaker = null), true),
		Pair(LyricLine(start = 158200uL, text = """So much to do so much to see""", words = null, speaker = null), false),
		Pair(LyricLine(start = 158200uL, text = """那么多事要做 那么多话要说""", words = null, speaker = null), true),
		Pair(LyricLine(start = 160500uL, text = """So what's wrong with takin the backstreets""", words = null, speaker = null), false),
		Pair(LyricLine(start = 160500uL, text = """让我们代替后街男孩又如何""", words = null, speaker = null), true),
		Pair(LyricLine(start = 163500uL, text = """You'll never know if you don't go""", words = null, speaker = null), false),
		Pair(LyricLine(start = 163500uL, text = """你永远不会知道，如果你不去尝试""", words = null, speaker = null), true),
		Pair(LyricLine(start = 165500uL, text = """You'll never shine if you don't glow""", words = null, speaker = null), false),
		Pair(LyricLine(start = 165500uL, text = """如果你没有激情，你永远不会发光""", words = null, speaker = null), true),
		Pair(LyricLine(start = 168100uL, text = """Hey now, you're an All Star, get your game on, go play""", words = null, speaker = null), false),
		Pair(LyricLine(start = 168100uL, text = """嘿 现在 你是一个全明星 去打你的比赛吧""", words = null, speaker = null), true),
		Pair(LyricLine(start = 173100uL, text = """Hey now, you're a Rock Star, get the show on, get paid,""", words = null, speaker = null), false),
		Pair(LyricLine(start = 173100uL, text = """嘿 现在 你是一个摇滚明星 去表演你的节目吧""", words = null, speaker = null), true),
		Pair(LyricLine(start = 177100uL, text = """And all that glitters is gold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 177100uL, text = """发光的都是金子""", words = null, speaker = null), true),
		Pair(LyricLine(start = 181100uL, text = """Only shootin stars break the mold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 181100uL, text = """你只需要打破常规""", words = null, speaker = null), true),
		Pair(LyricLine(start = 186100uL, text = """And all that glitters is gold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 186100uL, text = """发光的都是金子""", words = null, speaker = null), true),
		Pair(LyricLine(start = 190100uL, text = """Only shootin stars break the mold""", words = null, speaker = null), false),
		Pair(LyricLine(start = 190100uL, text = """你只需要打破常规""", words = null, speaker = null), true),
	)

}